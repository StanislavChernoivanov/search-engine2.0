package searchengine.dto.indexPage;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.URL;
import java.util.*;
public class LemmaFinder {
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static Connection con;

    private static final LuceneMorphology RUSSIAN_MORPHOLOGY;

    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

