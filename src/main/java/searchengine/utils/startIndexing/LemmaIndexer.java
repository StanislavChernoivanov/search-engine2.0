package searchengine.utils.startIndexing;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;

import java.util.*;


@Log4j2
public class LemmaIndexer implements Runnable {

    @Getter
    private final Site site;
    private final SaverOrRefresher saverOrRefresher;
    private final List<Page> pageList;
    public LemmaIndexer(Site site, SaverOrRefresher saverOrRefresher, PageRepository pageRepository) {
        this.site = site;
        this.saverOrRefresher = saverOrRefresher;
        pageList = pageRepository.findPageBySiteId(site.getId());
    }

    public void run() {
        pageList.forEach(p -> {
            Map<String, Integer> lemmas = new LemmaCollector()
                    .collectLemmas(clearTags(p.getContent()));
            lemmas.keySet().forEach(k -> saveOrRefresh(k, lemmas.get(k), p));
        });
    }

    private void updateLemma(Lemma lemma, float rank, Page page) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        saverOrRefresher.saveData(lemma, index);
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
        saverOrRefresher.saveData(lemma, index);
    }

    public void saveOrRefresh
            (String key, Integer rank, Page page) {
        try {
            Lemma lemma = null;
            Optional<Lemma> optionalLemma = saverOrRefresher.checkBuffer(key);
            if (optionalLemma.isPresent()) {
                lemma = optionalLemma.get();
                updateLemma(lemma, rank, page);
            } else {
                optionalLemma = saverOrRefresher.getOptionalLemma(key, page.getSite().getId());
                if (optionalLemma.isPresent()) {
                    lemma = optionalLemma.get();
                    updateLemma(lemma, rank, page);
                }
            }
            if (lemma != null) return;
            saveLemma(page, key, rank);
        } catch (Exception ex) {
            log.warn("{}\n{}\n{}", ex.getClass().getName(), ex.getMessage(), ex.getStackTrace());
        }
    }

    public String clearTags(String content) {
        StringBuilder builder = new StringBuilder();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.getAllElements();
        elements.forEach(e -> builder.append(e.ownText()).append(" "));
        return builder.toString();
    }
}

