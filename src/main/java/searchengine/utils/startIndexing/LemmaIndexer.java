package searchengine.utils.startIndexing;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;

import java.util.*;


@AllArgsConstructor
@Log4j2
public class LemmaIndexer implements Runnable {
    @Getter
    private final Page page;
    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;

    public void run() {
        Map<String, Integer> lemmas = new LemmaCollector().collectLemmas(clearTags(page.getContent()));
        lemmas.keySet().forEach(k -> insertInDataBase(k, lemmas));
    }
        private void insertInDataBase
                (String key, Map<String, Integer> lemmas) {
            try {
                if (lemmaRepository.lemmaIsExist(key, page.getSite().getId())) {
                    Lemma lemma = lemmaRepository.findLemma(key,
                            page.getSite().getId());
                    refresh(lemma, lemmas.get(key));
                } else {
                    save(page, key, lemmas.get(key));
                }
            } catch (DataIntegrityViolationException exception) {
                log.info("{}\n{}", exception.getMessage(), exception.getStackTrace());
            }
        }

    @Transactional
    private synchronized void save(Page page, String key, float rank) {
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
    @Transactional()
    private void refresh(Lemma lemma, float rank) {
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
}

