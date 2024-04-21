package searchengine.utils.startIndexing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class LemmaCollector {
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    public static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    private final Map<String, Integer> lemmas = new ConcurrentHashMap<>();

    private static LemmaCollector lemmaCollector;

    public static synchronized LemmaCollector getInstance() {
        if (lemmaCollector == null) {
            lemmaCollector = new LemmaCollector();
        }
        return lemmaCollector;

    }


    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> collectLemmas(String text) {
        clearMap();
        String[] words = arrayContainsRussianWords(text);
        for (String word : words) {
            if(word.length() < 2 || (word.length() == 2
                    && (word.contains("ь") || word.contains("ъ")))) continue;
            if (word.isBlank() || !word.matches("[а-яёА-ЯЁ]+")) continue;

            List<String> wordBaseForms = RUSSIAN_MORPHOLOGY.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) continue;
            List<String> normalForms = RUSSIAN_MORPHOLOGY.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }

    public void clearMap() {lemmas.clear();}

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    public synchronized boolean anyWordBaseBelongToParticle(String word) {
        return hasParticleProperty(word);
    }


    private boolean hasParticleProperty(String wordBase) {
        try {
            for (String property : particlesNames) {
                if (wordBase.toUpperCase().contains(property)) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException exception) {
            return  true;
        }
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
