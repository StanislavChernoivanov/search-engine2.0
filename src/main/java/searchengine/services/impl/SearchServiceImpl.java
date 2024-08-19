package searchengine.services.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.WrongCharaterException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.FailResponse;
import searchengine.dto.Response;
import searchengine.dto.search.Relevance;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.Snippet;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.SearchService;
import searchengine.utils.LemmaCollector;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexesRepository indexesRepository;
    private final LemmaCollector lemmaCollector = new LemmaCollector();
    private final Set<SearchResponse> containedMatchesResponses = new HashSet<>();
    private final Set<FailResponse> doNotContainedMatchesResponse = new HashSet<>();



    @Override
    public Response search(String query, String site, Integer offset, Integer limit) {
        if (offset == null) offset = 0;
        if (limit == null) limit = 0;
        if (query == null || query.isEmpty())
            return new FailResponse(false, "Empty request param specified");
        lemmaCollector.clear();
        if (siteRepository.getCountSites() == 0)
            return new FailResponse(false, "No site indexing, need to index");
        Set<String> lemmasFromQuerySet = lemmaCollector.collectLemmas(query).keySet();
        if (site == null) {
            return searchInSiteNotSpecifiedCase(lemmasFromQuerySet, offset, limit);
        } else {
            if (siteRepository.findSiteByUrl(site).isEmpty()) {
                clearMemory();
                return new FailResponse(false
                        , "Specified site is not found");
            }
            SearchResponse searchResponse = (SearchResponse)
                    countRelevanceAndGetResponse(lemmasFromQuerySet, site, limit).getResponse();
            if (searchResponse.getData() == null) {
                clearMemory();
                return searchResponse;
            }
            searchResponse.setData(getPartOfData(searchResponse.getData(), offset, limit));
            clearMemory();
            return limitTooManyPages(searchResponse, limit);
        }
    }

    private Response searchInSiteNotSpecifiedCase(Set<String> lemmasFromQuerySet,
                                                        Integer offset,
                                                        Integer limit) {
        List<Site> siteList = siteRepository.findAll();
        siteList.forEach(s -> {
            ResponseComparator comparator = countRelevanceAndGetResponse
                    (lemmasFromQuerySet, s.getUrl(), limit);
            if (comparator.isContainsMatches())
                containedMatchesResponses.add((SearchResponse) comparator.getResponse());
            else doNotContainedMatchesResponse.add((FailResponse) comparator.getResponse());
        });
        if (doNotContainedMatchesResponse.isEmpty()) {
            int countResults = containedMatchesResponses.stream().map(SearchResponse::getCount)
                    .reduce(Integer::sum).get();
            Set<SearchData> commonData = new TreeSet<>();
            containedMatchesResponses.stream().filter(r -> r.getData() != null)
                    .forEach(r -> commonData.addAll(r.getData()));
            SearchResponse searchResponse =
                    new SearchResponse(true, countResults, commonData);
            searchResponse.setData(getPartOfData(
                    searchResponse.getData(), offset, limit));
            clearMemory();
            return limitTooManyPages(searchResponse, limit);

        } else if (doNotContainedMatchesResponse.stream().
                anyMatch(response -> response.getError().matches("^[\\w\\s.]+\\([\\w\\s]+\\)$"))) {
            Response response = doNotContainedMatchesResponse.stream().
                    filter(failResponse -> failResponse.getError().
                            matches("^[\\w\\s.]+\\([\\w\\s]+\\)$")).findFirst().get();
            clearMemory();
            return response;

        } else if (!containedMatchesResponses.isEmpty()) {
            int countResults = containedMatchesResponses.stream().map(SearchResponse::getCount).
                    reduce(Integer::sum).get();
            Set<SearchData> commonData = new TreeSet<>();
            containedMatchesResponses.stream().filter(r -> r.getData() != null)
                    .forEach(r -> commonData.addAll(r.getData()));
            SearchResponse searchResponse =
                    new SearchResponse(true, countResults, commonData);
            searchResponse.setData(getPartOfData(
                    searchResponse.getData(), offset, limit));
            clearMemory();
            return limitTooManyPages(searchResponse, limit);
        } else {
            return doNotContainedMatchesResponse.stream().findFirst().get();
        }
    }

    private void clearMemory() {
        doNotContainedMatchesResponse.clear();
        containedMatchesResponses.clear();
    }


    private Response limitTooManyPages(SearchResponse searchResponse, Integer limit) {
        if (searchResponse.getData().size() > 50 && (limit > 50 || limit == 0)) {
            return new FailResponse(false,
                    "There are too much pages. " +
                            "Specify the limit value (no more than 50)");
        } else return searchResponse;
    }

    private ResponseComparator countRelevanceAndGetResponse
            (Set<String> lemmasFromQuery,
             String siteUrl, int limit) {
        Optional<Site> siteEntity = siteRepository.findSiteByUrl(siteUrl);
        if (siteEntity.isEmpty())
            return new ResponseComparator(new FailResponse(false
                    , "Specified site is not found"), false);
        int siteId = siteEntity.get().getId();
        Set<Lemma> lemmas = new TreeSet<>(
                lemmaRepository.findMatchingLemma(lemmasFromQuery.stream().toList(), siteId));
        if (lemmas.size() != lemmasFromQuery.size() || lemmas.isEmpty())
            return new ResponseComparator(new SearchResponse(true,
                    0, null), true);
        Optional<Lemma> lemma = lemmas.stream().findFirst();
        List<Page> pageSet = pageRepository.findPagesByLemma(lemma.get().getId());
        List<Page> pages = pageSet.stream().filter(p -> {
            boolean isContains = true;
            for (Lemma l : lemmas) {
                if (l.getLemma().equals(lemma.get().getLemma())) continue;
                if (indexesRepository.findOutNumberOfIndexesWithSpecifiedPageIdAndLemmaId(p.getId(), l.getId()) == 0)
                    isContains = false;
                break;
            }
            return isContains;
        }).toList();
        if (pages.isEmpty())
            return new ResponseComparator(new SearchResponse(true, 0, null), true);
        pageSet.clear();
        Map<Page, Float> relativeRelevance =
                Relevance.getRelativeRelevance(pages, pageRepository, siteId, limit, indexesRepository);
        if (!relativeRelevance.isEmpty())
            return new ResponseComparator(setResponse(relativeRelevance
                    , lemmasFromQuery, siteId, lemmas), true);
        else return new ResponseComparator(new FailResponse(false,
                "There are too much pages. " +
                        "Specify the limit value (no more than 50)"), false);
    }

    private SearchResponse setResponse(Map<Page, Float> relativeRelevance
            , Set<String> queryWords, int siteId, Set<Lemma> lemmas) {
        Set<SearchData> dataSet = new TreeSet<>();
        relativeRelevance.keySet().forEach(p -> {
            Document document = Jsoup.parse(p.getContent());
            Elements elements = document.body().getAllElements();
            TreeSet<Snippet> snippets = new TreeSet<>();
            int numberOfPages = pageRepository.findCountPagesBySiteId(siteId);
            elements.forEach(e -> {
                if (e.hasText()) {
                    handleElement(e, queryWords, snippets, siteId, lemmas, numberOfPages);
                }
            });
            dataSet.add(getSearchResultData(snippets
                    , relativeRelevance, p, document, queryWords));
        });
        return new SearchResponse(true,
                relativeRelevance.size(), dataSet);
    }

    private void handleElement(Element e, Set<String> queryWords
            , TreeSet<Snippet> snippets, int siteId, Set<Lemma> lemmas, int numberOfPages) {
        String[] words = e.text().split("[\\s+]");
        Map<Integer, String> wordsOfElement = new HashMap<>();
        int uniqueKey = 0;
        for (String s : words) {
            wordsOfElement.put(uniqueKey, s.replaceAll("\\p{P}", "")
                    .trim().toLowerCase(Locale.ROOT));
            uniqueKey++;
        }
        wordsOfElement.keySet().forEach(k -> {
            if (!wordsOfElement.get(k).matches("[а-яёa-z]+")
                    || wordsOfElement.get(k).length() < 2
                    || (wordsOfElement.get(k).length() == 2
                    && (wordsOfElement.get(k).contains("ь")
                    || wordsOfElement.get(k).contains("ъ")))) {
                wordsOfElement.put(k, "");
                return;
            }
            if (lemmaCollector.anyWordBaseBelongToParticle(wordsOfElement.get(k))) {
                wordsOfElement.put(k, "");
            }
        });
        wordsOfElement.keySet().removeIf(elem -> wordsOfElement.get(elem).isEmpty());
        Map<Integer, String> wordsOfElementContainedInQuery = new HashMap<>();
        for (String s : queryWords) {
            wordsOfElement.keySet().forEach(k -> {
                try {
                    if (wordsOfElement.get(k).matches(LemmaCollector.RUS_REGEX) &&
                            LemmaCollector.RUSSIAN_MORPHOLOGY.
                                    getNormalForms(wordsOfElement.get(k)).get(0).equals(s)) {
                        wordsOfElementContainedInQuery.put(k, wordsOfElement.get(k));
                    } else if (wordsOfElement.get(k).matches(LemmaCollector.ENG_REGEX)
                            && LemmaCollector.ENGLISH_MORPHOLOGY.
                            getNormalForms(wordsOfElement.get(k)).get(0).equals(s)) {
                        wordsOfElementContainedInQuery.put(k, wordsOfElement.get(k));
                    }
                } catch (WrongCharaterException ex) {
                    log.info("{}\n{}", ex.getMessage(), ex.getStackTrace());
                }
            });
        }
        snippets.addAll(getSnippets(words, wordsOfElementContainedInQuery, e, siteId, lemmas, numberOfPages));
    }

    private SearchData getSearchResultData(TreeSet<Snippet> snippets
            , Map<Page, Float> relativeRelevance, Page p, Document document
            , Set<String> queryWords) {
        List<Snippet> snippetList = new ArrayList<>(snippets);
        SearchData data = new SearchData();
        data.setRelevance(relativeRelevance.get(p));
        data.setSite(p.getSite().getUrl());
        data.setSiteName(p.getSite().getName());
        data.setUri(p.getPath());
        data.setTitle(document.title());
        data.setSnippet(limitNumberOfSnippets(snippetList, queryWords));
        return data;
    }

    private StringBuilder limitNumberOfSnippets(List<Snippet> snippetList, Set<String> queryWords) {
        StringBuilder snippetStrings = new StringBuilder();
        for (int i = 0; i < snippetList.size(); i++) {
            if (i == 0) snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
            else {
                if (i > 7) break;
                if (!Snippet.compareSnippets(snippetList.get(i), snippetList.get(i - 1)
                        , queryWords.size()))
                    snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
            }
        }
        return snippetStrings;
    }

    private Set<SearchData> getPartOfData(Set<SearchData> data,
                                          int offset, int limit) {
        List<SearchData> subList = data.stream()
                .toList().subList(offset < data.size() ? offset : 0
                        , data.size());
        if (limit == 0) return new TreeSet<>(subList);
        else {
            return subList.stream().limit(Math.min(limit, data.size()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private TreeSet<Snippet> getSnippets(String[] words, Map<Integer,
            String> wordsOfElementContainedInQuery,
                                         Element e, int siteId, Set<Lemma> lemmas, int numberOfPages) {
        wordsOfElementContainedInQuery.keySet().forEach(k -> words[k] = "<b>" + words[k] + "</b>");
        Set<Snippet> snippetSet = new TreeSet<>();
        List<Snippet> uniqueSnippetList = new ArrayList<>();
        wordsOfElementContainedInQuery.keySet()
                .forEach(k -> addSnippetInSnippetSet(words, k, e, snippetSet));
        List<Snippet> snippetList = new ArrayList<>(snippetSet);
        for (int i = 0; i < (snippetList.size() < 2 ? snippetSet.size() : snippetList.size() - 1); i++) {
            if (i == 0) uniqueSnippetList.add(snippetList.get(i));
            else if (StringUtils.countMatches(snippetList.get(i).getSnippet(), "<b>") == 1)
                uniqueSnippetList.add(snippetList.get(i));
            else if (!Arrays.equals(StringUtils
                            .substringsBetween(snippetList.get(i).getSnippet(), "<b>", "</b>")
                    , (StringUtils.substringsBetween(snippetList.get(i - 1)
                            .getSnippet(), "<b>", "</b>"))))
                uniqueSnippetList.add(snippetList.get(i));
        }
        return new TreeSet<>(Snippet.getSnippetsWithInfrequentlyRepeatedWords(uniqueSnippetList, siteId, lemmas, numberOfPages));
    }

    private void addSnippetInSnippetSet(String[] words, Integer k, Element e, Set<Snippet> snippetSet) {
        String snippet = "";
        if (words.length < 5) {
            for (int i = 0; i < words.length; i++) {
                if (i < e.text().split("[\\s+]").length - 1) {
                    snippet += words[i] + " ";
                } else snippet += words[i].trim().replaceAll("[.,!?;:]", "");
            }
        } else {
            if (k <= 2) snippet = String.format("%s %s %s %s %s%s",
                    words[0], words[1], words[2], words[3], words[4].trim()
                            .replaceAll("[.,!?;:]", ""), "...");
            else if (words.length - k <= 3) snippet = String.format("%s%s %s %s %s %s",
                    "...", words[words.length - 5].trim(), words[words.length - 4],
                    words[words.length - 3], words[words.length - 2],
                    words[words.length - 1].replaceAll("[.,!?;:]", ""));
            else snippet = String.format("%s%s %s %s %s %s%s",
                        "...", words[k - 2].trim(), words[k - 1],
                        words[k], words[k + 1], words[k + 2]
                                .trim().replaceAll("[.,!?;:]", ""), "...");
        }
        snippetSet.add(new Snippet(snippet, StringUtils.countMatches(snippet, "<b>")));
    }


    @RequiredArgsConstructor
    private static class ResponseComparator implements Comparable<ResponseComparator> {
        @Getter
        private final Response response;
        private final Boolean isContainsMatches;

        private boolean isContainsMatches() {
            return isContainsMatches;
        }

        @Override
        public int compareTo(ResponseComparator o) {
            if (response.result && o.response.result) return 0;
            else if (response.result) return -1;
            else if (o.response.result) return 1;
            else {
                FailResponse failResponse1 = (FailResponse) response;
                FailResponse failResponse2 = (FailResponse) o.response;
                return Integer.compare(failResponse1.getError().length(), failResponse2.getError().length());
            }
        }
    }

}
