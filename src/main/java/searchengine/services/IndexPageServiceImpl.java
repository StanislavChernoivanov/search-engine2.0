package searchengine.services;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import java.net.URL;
import java.util.*;

@Service
public class IndexPageServiceImpl  implements IndexPageService{

        private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        private static Connection con;
        @Autowired
        private LemmaRepository lemmaRepository;
        @Autowired
        private IndexesRepository indexesRepository;
        @Autowired
        private PageRepository pageRepository;
        @Autowired
        private SiteRepository siteRepository;

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
                                           LuceneMorphology luceneMorphology) throws IOException {
            IndexPageResponse response = new IndexPageResponse();
            String text = clearTags(url);
            Map<String, Integer> lemmas = collectLemmas(text, luceneMorphology);
            Page page = pageRepository.findPageByPath(url.getPath());
            if (page == null) {
                response.setResult(false);
                response.setError("Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
            } else {
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
            }
             return response;
        }

        public void indexPageInMultiThreaded(Site site, Page page, URL url) throws IOException {
            String text = clearTags(url);
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

        public static String clearTags(URL url) throws IOException {
            if (isUrl(url)) {
                StringBuilder builder = new StringBuilder();
                Document doc = con.
                        userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com").ignoreContentType(true).get();
                Elements elements = doc.getAllElements();
                elements.forEach(e -> builder.append(e.ownText()));
                return builder.toString();
            }
            return "";
        }

        public static boolean isUrl(URL url) {
            con = Jsoup.connect(url.toString());
            IndexPageResponse response = new IndexPageResponse();
            if(con.response().statusCode() == 404) {
                response.setError("Данная страница находится за пределами сайтов," +
                        " указанных в конфигурационном файле");
                response.setResult(false);
                return false;
            }
            else {
                return true;
            }
        }
}