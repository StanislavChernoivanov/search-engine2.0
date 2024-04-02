package searchengine.utils.startIndexing;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.repositories.LemmaRepository;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Log4j2
public class SaverOrRefresher {
    private final Set<Lemma> lemmaBuffer;
    private final LemmaRepository lemmaRepository;
    private final List<String> savedLemmaList;

    public SaverOrRefresher(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
        lemmaBuffer = new CopyOnWriteArraySet<>();
        savedLemmaList = new CopyOnWriteArrayList<>();
        Collections.sort(savedLemmaList);
    }

    public synchronized void saveOrRefresh
            (String key, Map<String, Integer> lemmas, Page page) {
        try {
            if (isExist(key, page)) {
                Lemma lemma;
                if(checkBuffer(key).isPresent())
                    updateLemma(checkBuffer(key).get(), lemmas.get(key), page);
                else {
                    Optional<Lemma> optionalLemma = Optional.ofNullable
                            (lemmaRepository.findLemma(key, page.getSite().getId()));
                    if (optionalLemma.isPresent()) {
                        lemma = optionalLemma.get();
                        updateLemma(lemma, lemmas.get(key), page);
                    }
                }
            } else {
                saveLemma(page, key, lemmas.get(key));
            }
        } catch (Exception ex) {
            log.warn("{}\n{}\n{}",ex.getClass().getName(), ex.getMessage(), ex.getStackTrace());
        }
    }

    private synchronized boolean isExist(String key, Page page) {
        int index = Collections.binarySearch(savedLemmaList
                , key + page.getSite().getId());
        if (index > 0) {
            return true;
        } else {
            savedLemmaList.add(key + page.getSite().getId());
            return false;
        }
    }

    private synchronized void updateLemma(Lemma lemma, float rank, Page page) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemma.getIndexesList().add(index);
        save(lemma);
    }

    private synchronized void save(Lemma lemma) {
        synchronized (lemmaRepository) {
        lemmaBuffer.add(lemma);
            if (lemmaBuffer.size() >= 500) {
                lemmaRepository.saveAll(lemmaBuffer);
                lemmaBuffer.clear();
            }
        }
    }
    private synchronized void saveLemma(Page page, String key, float rank) {
        Lemma lemma = new Lemma();
        lemma.setFrequency(1);
        lemma.setSite(page.getSite());
        lemma.setLemma(key);
        Indexes index = new Indexes();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        lemma.setIndexesList(new ArrayList<>());
        lemma.getIndexesList().add(index);
        save(lemma);
    }

    public synchronized void saveRemainedLemmasInDB() {
        if(!lemmaBuffer.isEmpty()) {
            lemmaRepository.saveAllAndFlush(lemmaBuffer);
            lemmaBuffer.clear();
        }
    }

    private synchronized Optional<Lemma> checkBuffer(String key) {
        return lemmaBuffer.stream().filter(l -> l.getLemma().equals(key)).findFirst();
    }
}
