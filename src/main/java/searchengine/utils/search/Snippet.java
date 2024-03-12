package searchengine.utils.search;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Getter
public class Snippet implements Comparable<Snippet> {
    private final String snippet;
    private final int numberOfMatchingWords;
    @Override
    public int compareTo(Snippet o) {
        int lengthThis = this.snippet.replaceAll("[^а-яёА-ЯЁ]", "").length();
        int lengthO = o.snippet.replaceAll("[^а-яёА-ЯЁ]", "").length();
        return Integer.compare(o.numberOfMatchingWords * 100 + lengthO,
                numberOfMatchingWords * 100 + lengthThis);
    }

    public static boolean compareSnippets(Snippet first, Snippet second, int countOfLemmaInQuery) {
        String[] wordsArrayOfFirstSnippet =
                StringUtils.substringsBetween(first.getSnippet(), "<b>", "</b>");
        String[] wordsArrayOfSecondSnippet =
                StringUtils.substringsBetween(second.getSnippet(), "<b>", "</b>");
        AtomicInteger counter = new AtomicInteger();
        Arrays.stream(wordsArrayOfFirstSnippet).toList().forEach(s -> {
            for (String snip : wordsArrayOfSecondSnippet) {
                if (s.replaceAll("[.,!?;:]", "")
                    .equals(snip.replaceAll("[.,!?;:]", "")))
                    counter.getAndIncrement();
            }
        });
        if(countOfLemmaInQuery < 5) return countOfLemmaInQuery - counter.get() <= 1;
        else return countOfLemmaInQuery - counter.get() <= 2;
    }
}
