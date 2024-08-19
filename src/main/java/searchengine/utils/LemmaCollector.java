package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class LemmaCollector {
    private static final String[] commonParticlesNames
            = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "PREP",
            "PN_ADJ", "PN pers", "CONJ, VBE"};

    public static final String RUS_REGEX;
    public static final String ENG_REGEX;
    public static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    public static final LuceneMorphology ENGLISH_MORPHOLOGY;


    private final Map<String, Integer> lemmas = new HashMap<>();


    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
            ENGLISH_MORPHOLOGY = new EnglishLuceneMorphology();
            RUS_REGEX = "^\\p{InCyrillic}{2,}$";
            ENG_REGEX = "^\\p{sc=Latin}{2,}$";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> collectLemmas(String text) {
        clear();
        String[] words = arrayContainsWords(text);
        for (String word : words) {
            if (word.matches(RUS_REGEX)) russianWordProcessing(word);
            else if (word.matches(ENG_REGEX)) englishWordProcessing(word);
        }
        return lemmas;
    }

    private void russianWordProcessing(String word) {
        if (word.length() == 2
                && (word.contains("ь") || word.contains("ъ"))) return;
        if (word.isBlank() || !word.matches("[а-яёА-ЯЁ]+")) return;
        List<String> wordBaseForms = RUSSIAN_MORPHOLOGY.getMorphInfo(word);
        if (anyWordBaseBelongToParticle(wordBaseForms)) return;
        List<String> normalForms = RUSSIAN_MORPHOLOGY.getNormalForms(word);
        if (normalForms.isEmpty()) return;
        String normalWord = normalForms.get(0);
        if (lemmas.containsKey(normalWord)) {
            lemmas.put(normalWord, lemmas.get(normalWord) + 1);
        } else {
            lemmas.put(normalWord, 1);
        }
    }

    private void englishWordProcessing(String word) {
        try {
            List<String> wordBaseForms = ENGLISH_MORPHOLOGY.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) return;
            List<String> normalForms = ENGLISH_MORPHOLOGY.getNormalForms(word);
            if (normalForms.isEmpty()) return;
            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        } catch (WrongCharaterException e) {
            log.debug("{}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public void clear() {
        lemmas.clear();
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    public synchronized boolean anyWordBaseBelongToParticle(String word) {
        return hasParticleProperty(word);
    }


    private boolean hasParticleProperty(String wordBase) {
        try {
            for (String property : commonParticlesNames) {
                if (wordBase.toUpperCase().contains(property)) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException exception) {
            return true;
        }
    }

    private String[] arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^a-zа-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
