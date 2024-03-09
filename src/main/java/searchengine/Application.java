package searchengine;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@SpringBootApplication
@Log4j2
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
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
//    @PostConstruct
//    public Map<String, Integer> collectLemmas() {
//        String text = "Регулярные выражения — тема, " +
//                "которую программисты, даже опытные, зачастую откладывают на потом." +
//                "Однако большинству Java-разработчиков рано или поздно придётся столкнуться " +
//                "с обработкой текстовой информации.";
//        String[] words = arrayContainsRussianWords(text);
//        log.error("{}\n{}", "Words of the text:", words);
//        HashMap<String, Integer> lemmas = new HashMap<>();
//        for (String word : words) {
//            if (word.isBlank()) {
//                continue;
//            }
//            List<String> wordBaseForms = RUSSIAN_MORPHOLOGY.getMorphInfo(word);
//            if (anyWordBaseBelongToParticle(wordBaseForms)) {
//                continue;
//            }
//            List<String> normalForms = RUSSIAN_MORPHOLOGY.getNormalForms(word);
//            RUSSIAN_MORPHOLOGY.
//            log.info(normalForms);
//            if (normalForms.isEmpty()) {
//                continue;
//            }
//            log.info("{} {}{}\n{}", "Normal forms of word ", word, ":", normalForms);
//            String normalWord = normalForms.get(0);
//            if (lemmas.containsKey(normalWord)) {
//                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
//            } else {
//                lemmas.put(normalWord, 1);
//            }
//        }
//        return lemmas;
//    }
//
//    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
//        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
//    }
//    private boolean hasParticleProperty(String wordBase) {
//        for (String property : particlesNames) {
//            if (wordBase.toUpperCase().contains(property)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private String[] arrayContainsRussianWords(String text) {
//        return text.toLowerCase(Locale.ROOT)
//                .replaceAll("([^а-я\\s])", " ")
//                .trim()
//                .split("\\s+");
//    }
}
