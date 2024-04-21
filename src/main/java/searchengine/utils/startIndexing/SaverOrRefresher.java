package searchengine.utils.startIndexing;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class SaverOrRefresher {
    private final Set<Lemma> lemmaBuffer;
    private final Set<Indexes> indexBuffer;
    private final LemmaRepository lemmaRepository;

    private final Lock lock;

    private final IndexesRepository indexesRepository;


    public SaverOrRefresher(LemmaRepository lemmaRepository, IndexesRepository indexesRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexesRepository = indexesRepository;
        lemmaBuffer = new CopyOnWriteArraySet<>();
        indexBuffer = new HashSet<>();
        lock = new ReentrantLock();
    }

    public void saveOrRefresh
            (String key, Integer rank, Page page) {
        lock.lock();
        try {
            Lemma lemma = null;
            if(checkBuffer(key).isPresent())
                updateLemma(checkBuffer(key).get(), rank, page);
            else {
                Optional<Lemma> optionalLemma = Optional.ofNullable
                        (lemmaRepository.findLemma(key, page.getSite().getId()));
                if (optionalLemma.isPresent()) {
                    lemma = optionalLemma.get();
                    updateLemma(lemma, rank, page);
                }
            }
            if(lemma != null) return;
            saveLemma(page, key, rank);
        } catch (Exception ex) {
            log.warn("{}\n{}\n{}",ex.getClass().getName(), ex.getMessage(), ex.getStackTrace());
        } finally {
            lock.unlock();
        }
    }

    private void updateLemma(Lemma lemma, float rank, Page page) {
        lock.lock();
        try {
            lemma.setFrequency(lemma.getFrequency() + 1);
            Indexes index = new Indexes();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(rank);
            save(lemma, index);
        } finally {
            lock.unlock();
        }
    }

    private void save(Lemma lemma, Indexes index) {
        lemmaBuffer.add(lemma);
        indexBuffer.add(index);
        lock.lock();
        try {
            if (lemmaBuffer.size() >= 300) {
                lemmaRepository.saveAll(lemmaBuffer);
                indexesRepository.saveAll(indexBuffer);
                lemmaBuffer.clear();
                indexBuffer.clear();
            }
        } finally {
            lock.unlock();
        }
    }
    private void saveLemma(Page page, String key, float rank) {
        lock.lock();
        try {
            Lemma lemma = new Lemma();
            lemma.setFrequency(1);
            lemma.setSite(page.getSite());
            lemma.setLemma(key);
            Indexes index = new Indexes();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(rank);
            save(lemma, index);
        } finally {
            lock.unlock();
        }
    }

    public void saveRemainedLemmasInDB() {
        if(!lemmaBuffer.isEmpty()) {
            lemmaRepository.saveAllAndFlush(lemmaBuffer);
            lemmaBuffer.clear();
        }
        if(!indexBuffer.isEmpty()) {
            indexesRepository.saveAllAndFlush(indexBuffer);
            indexBuffer.clear();
        }
    }

    private Optional<Lemma> checkBuffer(String key) {
        return lemmaBuffer.stream().filter(l -> l.getLemma().equals(key)).findFirst();
    }
}
