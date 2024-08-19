package searchengine.utils.startIndexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.utils.LemmaCollector;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Log4j2
@RequiredArgsConstructor
public class LemmaCreator implements Runnable {

    @Getter
    private final Site site;
    private final LemmaHandler lemmaHandler;
    private final List<Page> pageList;


    public void run() {
        pageList.forEach(p -> {
            Map<String, Integer> lemmas = new LemmaCollector()
                    .collectLemmas(handleContent(p.getContent()));
            lemmas.keySet().forEach(k -> saveOrUpdate(k, lemmas.get(k), p));
        });
    }

    public void saveOrUpdate
            (String key, Integer rank, Page page) {
        Lemma lemma = null;
        Optional<Lemma> optionalLemma = lemmaHandler.checkBuffer(key);
        if (optionalLemma.isPresent()) {
            lemma = optionalLemma.get();
            updateLemma(lemma, rank, page);
        } else {
            optionalLemma = lemmaHandler.getOptionalLemma(key, page.getSite().getId());
            if (optionalLemma.isPresent()) {
                lemma = optionalLemma.get();
                updateLemma(lemma, rank, page);
            }
        }
        if (lemma != null) return;
        saveLemma(page, key, rank);
    }

    private void updateLemma(Lemma lemma, float rank, Page page) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemmaHandler.saveOrUpdateLemma(lemma, index);
    }

    private void saveLemma(Page page, String key, float rank) {
        Lemma lemma = new Lemma();
        lemma.setFrequency(1);
        lemma.setSite(site);
        lemma.setLemma(key);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemmaHandler.saveOrUpdateLemma(lemma, index);
    }

    public String handleContent(String content) {
        StringBuilder builder = new StringBuilder();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.getAllElements();
        elements.forEach(e -> builder.append(e.ownText()).append(" "));
        return builder.toString();
    }
}

