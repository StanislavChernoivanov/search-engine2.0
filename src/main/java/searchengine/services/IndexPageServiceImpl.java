package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexPage.IndexPageResponse;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IndexPageServiceImpl  implements IndexPageService{

        private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        private static Connection con;
        private final LemmaRepository lemmaRepository;
        private final IndexesRepository indexesRepository;
        private final PageRepository pageRepository;
        private final SiteRepository siteRepository;

        private static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IndexPageResponse indexPage(URL url,
                                       LuceneMorphology luceneMorphology) {
        IndexPageResponse response = new IndexPageResponse();
    String text;
    try {
        text = (String) clearTags(url);
    } catch (NoSuchElementException ex) {
        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
        return response;
    }
    Map<String, Integer> lemmas = collectLemmas(text, RUSSIAN_MORPHOLOGY);
    Optional <Page> optionalPage = Optional.ofNullable(pageRepository.findPageByPath(url.getPath()));
    if (optionalPage.isEmpty()) {
        Page page = new Page();
        page.setPath(url.getPath());
        // TODO: 07.12.2023 Later
    }
    Page page = optionalPage.get();
    Optional<Site> siteOptional = siteRepository.findById(page.getSite().getId());
    if (siteOptional.isEmpty()) {
        throw new NullPointerException("Сайт не найден!");
    } else {
        Site site = siteOptional.get();
        Set<String> keySet = lemmas.keySet();
        for (String key : keySet) {
            Lemma lemma;
            Indexes index;
            if(!lemmaRepository.lemmaIsExist(key)) {
                lemma = new Lemma();
                lemma.setSite(site);
                lemma.setLemma(key);
                lemma.setFrequency(1);
                index = new Indexes();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(lemmas.get(key));
                indexesRepository.save(index);
            } else if (!indexesRepository.indexIsExist(lemmaRepository.
                    findLemma(key).getId(), page.getId())) {
                lemma = lemmaRepository.findLemma(key);
               lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = lemmaRepository.findLemma(key);
                lemma.setSite(site);
                lemma.setLemma(key);
                lemma.setFrequency(lemma.getFrequency());
            }
            lemmaRepository.save(lemma);
        }
        response.setError("");
        response.setResult(true);
    }
     return response;
    }

        private boolean isAvailableWebPage(URL url) {
            Connection connection = Jsoup.connect(url.toString());
            Document doc;
            try {
                doc = connection.
                        userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com").ignoreContentType(true).ignoreHttpErrors(true).get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

            public void indexPageInMultiThreaded(Site site, Page page, URL url) {
            String text = (String) clearTags(url);
            Map<String, Integer> lemmas = collectLemmas(text, RUSSIAN_MORPHOLOGY);
            Set<String> keySet = lemmas.keySet();
            for (String key : keySet) {
                Lemma lemma;
                Indexes index;
                if(!lemmaRepository.lemmaIsExist(key)) {
                    lemma = new Lemma();
                    lemma.setSite(site);
                    lemma.setLemma(key);
                    lemma.setFrequency(1);
                    index = new Indexes();
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setRank(lemmas.get(key));
                    indexesRepository.save(index);
                } else if (!indexesRepository.indexIsExist(lemmaRepository.
                        findLemma(key).getId(), page.getId())) {
                    lemma = lemmaRepository.findLemma(key);
                    lemma.setFrequency(lemma.getFrequency() + 1);
                } else {
                    lemma = lemmaRepository.findLemma(key);
                    lemma.setSite(site);
                    lemma.setLemma(key);
                    lemma.setFrequency(lemma.getFrequency());
                }
                lemmaRepository.save(lemma);
            }
        }

        public  boolean lemmaIsExist(String key) {
        return lemmaRepository.lemmaIsExist(key);
        }

        public Lemma findLemma(String key) {
        return lemmaRepository.findLemma(key);
        }

        public boolean indexIsExist(int lemmaId, int pageId) {
        return indexesRepository.indexIsExist(lemmaId, pageId);
        }

        public Map<String, Integer> collectLemmas(String text,
                                                  LuceneMorphology luceneMorphology) {
            String[] words = arrayContainsRussianWords(text);
            HashMap<String, Integer> lemmas = new HashMap<>();

            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }

                String normalWord = normalForms.get(0);

                if (lemmas.containsKey(normalWord)) {
                    lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1);
                }
            }

            return lemmas;
        }

        private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
            return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
        }
        private boolean hasParticleProperty(String wordBase) {
            for (String property : particlesNames) {
                if (wordBase.toUpperCase().contains(property)) {
                    return true;
                }
            }
            return false;
        }

        private String[] arrayContainsRussianWords(String text) {
            return text.toLowerCase(Locale.ROOT)
                    .replaceAll("([^а-я\\s])", " ")
                    .trim()
                    .split("\\s+");
        }
        private Document getDoc(Connection conn) throws IOException {
        return conn.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").ignoreContentType(true).ignoreHttpErrors(true).get();
        }

        public Object clearTags(URL url)  {
        con = Jsoup.connect(String.valueOf(url));
            if (!isUrl(url)) return false;
            StringBuilder builder = new StringBuilder();
            Document doc = null;
            try {
                // TODO: 06.12.2023 MUST WILL DO IT!!! 
                doc = this.getDoc(con);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<searchengine.config.Site> siteList = new SitesList().getSites();
            if (siteList.stream().noneMatch(s -> {
                try {
                    return new URL(s.getUrl()).getHost().equals(url.getHost());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            })) return false;
            Elements elements = doc.getAllElements();
            elements.forEach(e -> builder.append(e.ownText()).append(" "));
            return builder.toString();
        }

        public static boolean isUrl(URL url) {
            con = Jsoup.connect(url.toString());
            return !String.valueOf(con.response().statusCode()).substring(0, 1).matches("[45]");
        }
}