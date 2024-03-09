package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.startIndexing.LemmaCollector;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.search.Relevance;
import searchengine.search.SearchResponse;
import searchengine.search.SearchResultData;
import searchengine.search.Snippet;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaCollector lemmaCollector = new LemmaCollector();
    private int siteId = 0;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if(offset == null) offset = 0;
        if (limit == null) limit = 0;
        if (Objects.equals(query, ""))
            return getWrongResponse(false, "Задан пустой поисковый запрос");
        Set<Lemma> lemmas = new TreeSet<>(Comparator.comparingInt(Lemma::getFrequency));
        lemmaCollector.clearMap();
        Set<String> lemmasFromQuerySet = lemmaCollector.collectLemmas(query).keySet();
        if (siteRepository.findAll().isEmpty())
            return getWrongResponse(false, "Отсутствует индекс сайта. Необходимо провести индексацию");
        if (site == null) {
            Set<SearchResponse> wellResponseSet;
            SearchResponse searchResponseCommon = new SearchResponse();
            Set<SearchResponse> responseSet = new HashSet<>();
            List<Site> siteList = siteRepository.findAll();
            Integer finalOffset = offset;
            Integer finalLimit = limit;
            siteList.forEach(s ->
                responseSet.add(countRelevanceAndGetResponse
                (lemmas, lemmasFromQuerySet, s.getUrl(), finalOffset, finalLimit)));
            wellResponseSet = responseSet.stream()
                    .filter(response -> response.getCount() != 0)
                    .collect(Collectors.toSet());
            if(wellResponseSet.size() == responseSet.size()) {
                searchResponseCommon.setResult(true);
                searchResponseCommon.setCount
                    (wellResponseSet.stream().map(SearchResponse::getCount).reduce(Integer::sum).get());
                Set<SearchResultData> commonSearchResultData = new TreeSet<>();
                wellResponseSet.forEach(r -> commonSearchResultData.addAll(r.getSearchResultData()));
                searchResponseCommon.setSearchResultData(commonSearchResultData);
                return searchResponseCommon;
            } else if(responseSet.size() - wellResponseSet.size() != responseSet.size()) {
                searchResponseCommon.setResult(true);
                searchResponseCommon.setCount
                    (wellResponseSet.stream().map(SearchResponse::getCount).
                    reduce(Integer::sum).get());
                Set<SearchResultData> commonSearchResultData = new TreeSet<>();
                wellResponseSet.stream().filter(r -> r.getCount() > 0)
                    .forEach(r -> commonSearchResultData.addAll(r.getSearchResultData()));
                searchResponseCommon.setSearchResultData(commonSearchResultData);
                return searchResponseCommon;
            } else {
                return responseSet.stream().filter(r -> !r.getError()
                        .isEmpty()).findFirst().orElse(
                        responseSet.stream().filter(
                        r -> r.getError().isEmpty() && r.getCount() == 0).findFirst().get());
            }
        } else {
                return countRelevanceAndGetResponse(lemmas, lemmasFromQuerySet, site, offset, limit);
        }
    }

    // TODO: 09.03.2024 dont get snippet on request like "Историк по образованию" 
    private SearchResponse countRelevanceAndGetResponse
                                        (Set<Lemma> lemmas, Set<String> lemmasFromQuerySet,
             String site, int offset, int limit) {
        Optional<Site> siteEntity = siteRepository.findSiteByUrl(site);
        if(siteEntity.isEmpty())
            return getWrongResponse(false, "Указанный сайт не найден");

        siteId = siteEntity.get().getId();
        for (String s : lemmasFromQuerySet) {
            Optional<Lemma> optionalLemma = Optional.ofNullable
                    (lemmaRepository.findLemma(s, siteEntity.get().getId()));
            optionalLemma.ifPresent(lemmas::add);
        }
        if(lemmas.isEmpty())
            return getWrongResponse(true, "");

        Set<Page> pageSet = new HashSet<>();
        Optional<Lemma> lemma = lemmas.stream().findFirst();
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
            return getWrongResponse(true, "");
        else if(pages.size() > 50)
            return getWrongResponse(false, "Найдено слишком много страниц. " +
                    "Введите больше слов для уточнения");

        pageSet.clear();
        Map<Page, Float> absoluteRelevanceMap = new HashMap<>();
        pages.forEach(p -> {
            Optional<Float> summ = p.getIndexList().stream().
                    filter(i -> i.getPage().getPath().equals(p.getPath()))
                    .map(index -> {
                        float commonPercent = (float) index.getLemma().getFrequency() /
                                pageRepository.findCountPagesBySiteId(siteId) * 100;
                        if(commonPercent < 30) return index.getRank();
                        else return 0.0f;
                    }).reduce(Float::sum);
            summ.ifPresent(aFloat -> absoluteRelevanceMap.put(p, aFloat));
        });
        Relevance relevance = new Relevance(absoluteRelevanceMap);
        Map<Page, Float> relativeRelevance = relevance.getRelativeRelevance();
        return setResponse(relativeRelevance, lemmasFromQuerySet, offset, limit);
    }

    private SearchResponse getWrongResponse(boolean result, String errorText) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(result);
        searchResponse.setError(errorText);
        return searchResponse;
    }
    private SearchResponse getWellResponse(int count, Set<SearchResultData> searchResultData) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setError("");
        searchResponse.setCount(count);
        searchResponse.setSearchResultData(searchResultData);
        return searchResponse;
    }

    private SearchResponse setResponse(Map<Page, Float> relativeRelevance
                                        , Set<String> queryWords
                                        , int offset, int limit) {
        Set<SearchResultData> searchResultDataSet = new TreeSet<>();
        relativeRelevance.keySet().forEach(p -> {
            StringBuilder snippetStrings = new StringBuilder();
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
            List<Snippet> snippetList = new ArrayList<>(snippets);
            for(int i = 0; i < snippetList.size(); i++) {
                if(i == 0)snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
                else {
                    if(i > 10) break;
                    if(!Snippet.compareSnippets(snippetList.get(i), snippetList.get(i - 1)
                        , queryWords.size()))
                        snippetStrings.append(snippetList.get(i).getSnippet()).append(", ");
                }
            }
            System.out.println(snippetStrings);
            SearchResultData searchResultData = new SearchResultData();
            searchResultData.setRelevance(relativeRelevance.get(p));
            searchResultData.setSite(p.getSite().getUrl());
            searchResultData.setSiteName(p.getSite().getName());
            searchResultData.setUri(p.getPath());
            searchResultData.setTitle(document.title());
            searchResultData.setSnippet(snippetStrings);
            searchResultDataSet.add(searchResultData);
        });
        return getWellResponse(relativeRelevance.size()
            , getPartOfData(searchResultDataSet, offset, limit));
    }

    private Set<SearchResultData> getPartOfData(Set<SearchResultData> searchResultData,
                                                int offset, int limit) {
        if (limit == 0)
            return new TreeSet<>(searchResultData.stream()
                .toList().subList(offset < searchResultData.size() ? offset : 0
                , searchResultData.size()));
        else {
            List<SearchResultData> subList = searchResultData.stream()
                    .toList().subList(offset < searchResultData.size() ? offset : 0
                    , searchResultData.size());
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
        for(int i = 0; i < snippetSet.size() - 1; i++) {
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
            if(s.getNumberOfMatchingWords() > 1) goodSnippets.add(s);
            else {
                String makeLemmaFromSnippetWord = new LemmaCollector().collectLemmas(
                    StringUtils.substringBetween(s.getSnippet(), "<b>", "</b>")
                    .replaceAll("\\p{P}", "")
                    .trim()).keySet().stream().findFirst().get();
                float commonPercent = (float) lemmaRepository.findLemmaBySiteIdAndByLemma(siteId,
                    makeLemmaFromSnippetWord).getFrequency() /
                    pageRepository.findCountPagesBySiteId(siteId) * 100;
                if(commonPercent < 30) goodSnippets.add(s);
            }
        });
     return goodSnippets;
    }
}
