package searchengine.dto.indexPage;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.startIndexing.HibernateUtil;
import searchengine.dto.startIndexing.SiteParser;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class LemmaIndexer implements Callable<Integer> {

    private static final Logger LOGGER = LogManager.getLogger(LemmaIndexer.class);
    @Getter
    private final Page page;
    private static final SessionFactory factory = HibernateUtil.getSESSION_FACTORY();
    private final Session session;

    public LemmaIndexer(Page page) {

        this.page = page;
        session = factory.openSession();
    }
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer call() throws InterruptedException {
        int i = 0;
        Map<String, Integer> lemmas = collectLemmas(clearTags(page.getContent()));
        Set<String> keySet = lemmas.keySet();
        try {
            for (String key : keySet) {
                session.beginTransaction();
                insertInDataBase(key, lemmas);
                session.getTransaction().commit();
            }
        } catch (Exception ex) {
            LOGGER.error("{} \n{}", ex.getMessage(), ex.getStackTrace());
            session.getTransaction().rollback();
        } finally {
            session.close();
        }
        return i;
    }

    private void insertInDataBase(String key, Map<String, Integer> lemmas) {
        Optional<Lemma> optionalLemma = HibernateUtil.findLemma(key, session);
        if (optionalLemma.isPresent() && optionalLemma.get().getId() != 0) {
            Lemma lemma = optionalLemma.get();
            lemma.setFrequency(lemma.getFrequency() + 1);
            session.saveOrUpdate(lemma);
            Indexes index = new Indexes();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(lemmas.get(key));
            session.persist(index);
        } else {
            Lemma lemma = new Lemma();
            lemma.setSite(page.getSite());
            lemma.setLemma(key);
            lemma.setFrequency(1);
            session.persist(lemma);
            Indexes index = new Indexes();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(lemmas.get(key));
            session.persist(index);
        }
    }


    public static String clearTags(String content) {
        StringBuilder builder = new StringBuilder();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.getAllElements();
        elements.forEach(e -> builder.append(e.ownText()).append(" "));
        return builder.toString();
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordBaseForms = RUSSIAN_MORPHOLOGY.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }
            List<String> normalForms = RUSSIAN_MORPHOLOGY.getNormalForms(word);
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

    public static boolean isUrl(URL url) {
        Connection con = Jsoup.connect(url.toString());
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

