package searchengine.dto.search;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import searchengine.model.entities.Lemma;
import searchengine.utils.startIndexing.LemmaCollector;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
@RequiredArgsConstructor
@Getter
@Slf4j
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

    public static TreeSet<Snippet> getSnippetsWithInfrequentlyRepeatedWords(
            List<Snippet> snippets
            , int siteId
            , Set<Lemma> lemmas
            , int numberOfPages) {
        TreeSet<Snippet> goodSnippets = new TreeSet<>();
                snippets.forEach(s -> {
            if(snippets.size() == 1) goodSnippets.add(s);
            else if(s.getNumberOfMatchingWords() > 1) goodSnippets.add(s);
            else {
                String makeLemmaFromSnippetWord = new LemmaCollector().collectLemmas(
                        StringUtils.substringBetween(s.getSnippet(), "<b>", "</b>")
                                .replaceAll("\\p{P}", "")
                                .trim()).keySet().stream().findFirst().get();
                try {
                    float commonPercent = 0.0f;
                    try {
                        commonPercent = (float) lemmas.stream()
                                .filter(l -> l.getLemma()
                                        .equals(makeLemmaFromSnippetWord))
                                .findFirst().get()
                                .getFrequency() /
                                numberOfPages * 100;
                    } catch (NoSuchElementException e) {
                        log.info("word from snippet - {}, lemmas: - {}", makeLemmaFromSnippetWord
                                , Arrays.toString(lemmas.stream().map(Lemma::getLemma).distinct().toArray()));
                    }
                    if(commonPercent < 30) goodSnippets.add(s);
                } catch (NullPointerException ex) {
                    log.warn("site id - {}, lemma - {} ", siteId, makeLemmaFromSnippetWord);
                }
            }
        });
        return goodSnippets;
    }
}
