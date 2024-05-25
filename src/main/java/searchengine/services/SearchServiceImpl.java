package searchengine.services;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.utils.startIndexing.LemmaCollector;
import searchengine.utils.Response;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.utils.search.Relevance;
import searchengine.utils.search.SearchResponse;
import searchengine.utils.search.SearchResultData;
import searchengine.utils.search.Snippet;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;


@Service
@Log4j2
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaCollector lemmaCollector = new LemmaCollector();
    private final Set<SearchResponse> containedMatchesResponses;
    private final Set<Response> doNotContainedMatchesResponses;
    private int siteId = 0;

    public SearchServiceImpl(LemmaRepository lemmaRepository, SiteRepository siteRepository, PageRepository pageRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        containedMatchesResponses = new HashSet<>();
        doNotContainedMatchesResponses = new HashSet<>();
    }


    @Override
    public Response search(String query, String site, Integer offset, Integer limit) {
        if(offset == null) offset = 0;
        if (limit == null) limit = 0;
        if (query == null)
            return new Response(false, "Задан пустой " +
                    "поисковый запрос");
        lemmaCollector.clearMap();
        Set<String> lemmasFromQuerySet = lemmaCollector.collectLemmas(query).keySet();
        if (siteRepository.findAll().isEmpty())
            return new Response(false, "Отсутствует индекс сайта. " +
                    "Необходимо провести индексацию");
        if (site == null) {
            List<Site> siteList = siteRepository.findAll();
            Integer finalLimit = limit;
            siteList.forEach(s -> {
                ResponseClass responseClass = countRelevanceAndGetResponse
                        (lemmasFromQuerySet, s.getUrl(), finalLimit);
                if(responseClass.getIsContainsMatches())
                    containedMatchesResponses.add((SearchResponse) responseClass.getResponse());
                else doNotContainedMatchesResponses.add(responseClass.getResponse());
            });
            if(doNotContainedMatchesResponses.isEmpty()) {
                int count = containedMatchesResponses.stream().map(SearchResponse::getCount)
                        .reduce(Integer::sum).get();
                Set<SearchResultData> commonSearchResultData = new TreeSet<>();
                containedMatchesResponses.stream().filter(r -> r.getSearchResultData() != null)
                        .forEach(r -> commonSearchResultData.addAll(r.getSearchResultData()));
                SearchResponse searchResponse =
                        new SearchResponse(true, "", count, commonSearchResultData);
                searchResponse.setSearchResultData(getPartOfData
                        (searchResponse.getSearchResultData(), offset, limit));
                clearMemory();
                return isFoundTooManyPages(searchResponse, limit);
            } else if(!containedMatchesResponses.isEmpty()) {
                int count = containedMatchesResponses.stream().map(SearchResponse::getCount).
                    reduce(Integer::sum).get();
                Set<SearchResultData> commonSearchResultData = new TreeSet<>();
                containedMatchesResponses.stream().filter(r -> r.getSearchResultData() != null)
                    .forEach(r -> commonSearchResultData.addAll(r.getSearchResultData()));
                SearchResponse searchResponse =
                        new SearchResponse(true, "", count, commonSearchResultData);
                searchResponse.setSearchResultData(getPartOfData
                        (searchResponse.getSearchResultData(), offset, limit));
                clearMemory();
                return isFoundTooManyPages(searchResponse, limit);
            } else {
                return doNotContainedMatchesResponses.stream().findFirst().get();
            }
        } else {
            if (siteRepository.findSiteByUrl(site).isEmpty()) {
                clearMemory();
                return new Response(false
                        , "Указанный сайт не найден");
            }
            SearchResponse searchResponse = (SearchResponse)
                    countRelevanceAndGetResponse(lemmasFromQuerySet, site, limit).getResponse();
            if(searchResponse.getSearchResultData() == null) {
                clearMemory();
                return searchResponse;
            }
            searchResponse.setSearchResultData(getPartOfData(searchResponse.getSearchResultData(), offset, limit));
            clearMemory();
                return isFoundTooManyPages(searchResponse, limit);
        }
    }

    private void clearMemory() {
        doNotContainedMatchesResponses.clear();
        containedMatchesResponses.clear();
    }


    private Response isFoundTooManyPages(SearchResponse searchResponse, Integer limit) {
        if(searchResponse.getSearchResultData().size() > 50 && (limit > 50 || limit == 0)) {
            return new Response(false,
                    "Найдено слишком много страниц. " +
                            "Уточните запрос или укажите значение limit (не более 50)");
        } else return searchResponse;
    }
    @RequiredArgsConstructor
    @Getter
    private static class ResponseClass implements Comparable<ResponseClass> {
        private  final Response response;
        private final Boolean isContainsMatches;

        @Override
        public int compareTo(ResponseClass o) {
            return Integer.compare(this.response.getError().length(), o.response.getError().length());
        }
    }

    private ResponseClass countRelevanceAndGetResponse
                                        (Set<String> lemmasFromQuerySet,
             String site, int limit) {
        Set<Lemma> lemmas = new TreeSet<>();
        Optional<Site> siteEntity = siteRepository.findSiteByUrl(site);
        if(siteEntity.isEmpty())
            return new ResponseClass(new Response(false
                    , "Указанный сайт не найден"), false);
        siteId = siteEntity.get().getId();
        for (String s : lemmasFromQuerySet) {
            Optional<Lemma> optionalLemma = Optional.ofNullable
                    (lemmaRepository.findLemma(s, siteId));
            optionalLemma.ifPresent(lemmas::add);
        }
        if(lemmas.size() != lemmasFromQuerySet.size())
            return new ResponseClass(new SearchResponse(true, "",
                    0, null), true);
        Set<Page> pageSet = new HashSet<>();
        Optional<Lemma> lemma = lemmas.stream().filter(lemma1 ->
                lemma1.getSite().getId() == siteId).findFirst();
        lemma.get().getIndexesList().forEach(i -> pageSet.add(i.getPage()));
        List<Page> pages = pageSet.stream().filter(p -> {
            boolean isContains = true;
            for(Lemma l:lemmas) {
                if (l.getIndexesList().stream().noneMatch(i -> i.getPage().getPath().equals(p.getPath())))
                    isContains = false;
            }
            return isContains;
        }).toList();
        if(pages.isEmpty())
            return new ResponseClass(new SearchResponse(true, "", 0, null), true);
        pageSet.clear();
        Map<Page, Float> relativeRelevance =
                        Relevance.getRelativeRelevance(pages, pageRepository, siteId, limit);
        if(!relativeRelevance.isEmpty())
            return new ResponseClass(setResponse(relativeRelevance
                    , lemmasFromQuerySet), true);
        else return new ResponseClass(new Response(false,
            "Найдено слишком много страниц. " +
            "Уточните запрос или укажите значение limit (не более 50)"), false);
    }

    private SearchResponse setResponse(Map<Page, Float> relativeRelevance
                                        , Set<String> queryWords) {
        Set<SearchResultData> searchResultDataSet = new TreeSet<>();
        relativeRelevance.keySet().forEach(p -> {
            Document document = Jsoup.parse(p.getContent());
            Elements elements = document.body().getAllElements();
            TreeSet<Snippet> snippets = new TreeSet<>();
                    elements.forEach(e -> {
                if(e.hasText()) {
                    String [] words= e.text().split("[\\s+]");
                    Map<Integer, String> wordsOfElement = new HashMap<>();
                    int uniqueKey = 0;
                    for(String s : words) {
                        wordsOfElement.put(uniqueKey, s.replaceAll("\\p{P}", "")
                                .trim().toLowerCase(Locale.ROOT));
                        uniqueKey++;
                    }
                    wordsOfElement.keySet().forEach(k -> {
                        if(!wordsOfElement.get(k).matches("[а-яё]+")
                                || wordsOfElement.get(k).length() < 2
                                || (wordsOfElement.get(k).length() == 2
                                && (wordsOfElement.get(k).contains("ь")
                                || wordsOfElement.get(k).contains("ъ")))) {
                            wordsOfElement.put(k, "");
                            return;
                        }
                        if(lemmaCollector.anyWordBaseBelongToParticle(wordsOfElement.get(k))
                        || Optional.ofNullable(LemmaCollector.RUSSIAN_MORPHOLOGY.
                            getNormalForms(wordsOfElement.get(k))).isEmpty()) {
                            wordsOfElement.put(k, "");
                        }
                    });
                    wordsOfElement.keySet().removeIf(elem-> wordsOfElement.get(elem).isEmpty());
                    Map<Integer, String> wordsOfElementContainedInQuery = new HashMap<>();
                    for(String s : queryWords) {
                        wordsOfElement.keySet().forEach(k ->  {
                            if(LemmaCollector.RUSSIAN_MORPHOLOGY.
                                getNormalForms(wordsOfElement.get(k)).get(0).equals(s)) {
                                wordsOfElementContainedInQuery.put(k, wordsOfElement.get(k));
                            }
                        });
                    }
                    snippets.addAll(getSnippets(words, wordsOfElementContainedInQuery, e));
                }
            });
            searchResultDataSet.add(getSearchResultData(snippets
                , relativeRelevance, p, document, queryWords));
        });
        return new SearchResponse(true, "",
                relativeRelevance.size(), searchResultDataSet);
    }

    private SearchResultData getSearchResultData(TreeSet<Snippet> snippets
            , Map<Page, Float> relativeRelevance, Page p, Document document
            , Set<String> queryWords) {
        List<Snippet> snippetList = new ArrayList<>(snippets);
        SearchResultData searchResultData = new SearchResultData();
        searchResultData.setRelevance(relativeRelevance.get(p));
        searchResultData.setSite(p.getSite().getUrl());
        searchResultData.setSiteName(p.getSite().getName());
        searchResultData.setUri(p.getPath());
        searchResultData.setTitle(document.title());
        searchResultData.setSnippet(limitNumberOfSnippets(snippetList, queryWords));
        return searchResultData;
    }

    private StringBuilder limitNumberOfSnippets(List<Snippet> snippetList, Set<String> queryWords) {
        StringBuilder snippetStrings = new StringBuilder();
        for(int i = 0; i < snippetList.size(); i++) {
            if(i == 0)snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
            else {
                if(i > 7) break;
                if(!Snippet.compareSnippets(snippetList.get(i), snippetList.get(i - 1)
                        , queryWords.size()))
                    snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
            }
        }
        return snippetStrings;
    }

    private Set<SearchResultData> getPartOfData(Set<SearchResultData> searchResultData,
                                                int offset, int limit) {
        List<SearchResultData> subList = searchResultData.stream()
                .toList().subList(offset < searchResultData.size() ? offset : 0
                        , searchResultData.size());
        if (limit == 0) return new TreeSet<>(subList);
        else {
            return subList.stream().limit(Math.min(limit, searchResultData.size()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private TreeSet<Snippet> getSnippets(String [] words, Map<Integer,
            String> wordsOfElementContainedInQuery,
                                         Element e) {
        wordsOfElementContainedInQuery.keySet().forEach(k -> words[k] = "<b>" + words[k] + "</b>");
        Set<Snippet> snippetSet = new TreeSet<>();
        List<Snippet> uniqueSnippetList = new ArrayList<>();
        wordsOfElementContainedInQuery.keySet().forEach(k -> {
            String snippet = "";
            if(words.length < 5) {
                for(int i = 0; i < words.length; i++) {
                    if(i < e.text().split("[\\s+]").length - 1) {
                        snippet += words[i] + " ";
                    } else snippet += words[i].trim().replaceAll("[.,!?;:]", "");
                }
            } else {
                if (k <= 2) snippet = String.format("%s %s %s %s %s%s",
                    words[0], words[1], words[2], words[3], words[4].trim()
                        .replaceAll("[.,!?;:]", ""), "...");
                else if (words.length - k <= 3)  snippet = String.format("%s%s %s %s %s %s",
                    "...", words[words.length - 5].trim(), words[words.length - 4],
                    words[words.length - 3], words[words.length - 2],
                    words[words.length - 1].replaceAll("[.,!?;:]", ""));
                else  snippet = String.format("%s%s %s %s %s %s%s",
                    "...", words[k - 2].trim(), words[k - 1],
                    words[k], words[k + 1], words[k + 2]
                        .trim().replaceAll("[.,!?;:]", ""), "...");
            }
            snippetSet.add(new Snippet(snippet, StringUtils.countMatches(snippet, "<b>")));
        });
        List<Snippet> snippetList = new ArrayList<>(snippetSet);
        for(int i = 0; i < (snippetList.size() < 2 ? snippetSet.size() : snippetList.size() - 1); i++) {
            if(i == 0) uniqueSnippetList.add(snippetList.get(i));
            else if(StringUtils.countMatches(snippetList.get(i).getSnippet(), "<b>") == 1)
                uniqueSnippetList.add(snippetList.get(i));
            else if(!Arrays.equals(StringUtils
                .substringsBetween(snippetList.get(i).getSnippet(), "<b>", "</b>")
                , (StringUtils.substringsBetween(snippetList.get(i - 1)
                .getSnippet(), "<b>", "</b>"))))
                uniqueSnippetList.add(snippetList.get(i));
        }
        return new TreeSet<>(getSnippetsWithoutTooCommonLemma(uniqueSnippetList));
    }

    private TreeSet<Snippet> getSnippetsWithoutTooCommonLemma(List<Snippet> snippets) {
        TreeSet<Snippet> goodSnippets = new TreeSet<>();
        snippets.forEach(s -> {
            if(snippets.size() == 1) goodSnippets.add(s);
            if(s.getNumberOfMatchingWords() > 1) goodSnippets.add(s);
            else {
                String makeLemmaFromSnippetWord = new LemmaCollector().collectLemmas(
                    StringUtils.substringBetween(s.getSnippet(), "<b>", "</b>")
                    .replaceAll("\\p{P}", "")
                    .trim()).keySet().stream().findFirst().get();
                try {
                    float commonPercent = (float) lemmaRepository.findLemmaBySiteIdAndLemma(siteId,
                        makeLemmaFromSnippetWord).getFrequency() /
                        pageRepository.findCountPagesBySiteId(siteId) * 100;
                    if(commonPercent < 30) goodSnippets.add(s);
                } catch (NullPointerException ex) {
                    log.info("site id - {}, lemma - {} ", siteId, makeLemmaFromSnippetWord);
                }
            }
        });
     return goodSnippets;
    }
}
