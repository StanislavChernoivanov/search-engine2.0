package searchengine.dto.indexPage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import java.io.IOException;
import java.util.*;



@RequiredArgsConstructor
public class LemmaIndexer implements Runnable {
    @Getter
    private final Page page;
    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;


    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        Map<String, Integer> lemmas = collectLemmas(clearTags(page.getContent()));
        lemmas.keySet().forEach(k -> insertInDataBase(k.trim(), lemmas));
    }

    private void insertInDataBase(String key, Map<String, Integer> lemmas) {
        try {
            Optional<Lemma> optionalLemma = Optional.ofNullable(lemmaRepository.findLemma(key));
            if (optionalLemma.isPresent()) {
                merge(optionalLemma, lemmas.get(key));
            }
            else {
                persist(page, key, lemmas.get(key));
            }
        } catch (DataIntegrityViolationException exception) {
            exception.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Transactional()
    private void persist(Page page, String key, float rank) {
        Lemma lemma = new Lemma();
        lemma.setFrequency(1);
        lemma.setSite(page.getSite());
        lemma.setLemma(key);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemmaRepository.save(lemma);
        indexesRepository.save(index);
    }
    @Transactional
    private void merge(Optional<Lemma> optionalLemma, float rank) {
        Lemma lemma = optionalLemma.get();
        lemma.setFrequency(lemma.getFrequency() + 1);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemmaRepository.save(lemma);
        indexesRepository.save(index);
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
}

