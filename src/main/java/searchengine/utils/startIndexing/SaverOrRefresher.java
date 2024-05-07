package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import java.util.*;

@Log4j2
public class SaverOrRefresher {
    private final Set<Lemma> lemmaBuffer;
    private final Set<Indexes> indexBuffer;
    @Getter
    private final LemmaRepository lemmaRepository;
    @Getter
    private final IndexesRepository indexesRepository;
    private static SaverOrRefresher saverOrRefresher;


    public static synchronized SaverOrRefresher getInstance
            (LemmaRepository lemmaRepository, IndexesRepository indexesRepository) {
        if(saverOrRefresher == null) {
            saverOrRefresher = new SaverOrRefresher(lemmaRepository, indexesRepository);
        }
        return saverOrRefresher;
    }

    private SaverOrRefresher(LemmaRepository lemmaRepository, IndexesRepository indexesRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexesRepository = indexesRepository;
        lemmaBuffer = new HashSet<>();
        indexBuffer = new HashSet<>();
    }

    synchronized Optional<Lemma> getOptionalLemma(String key, int siteId)  {
        return Optional.ofNullable(lemmaRepository.findLemma(key, siteId));
    }

    private synchronized void save() {
        if (lemmaBuffer.size() >= 300) {
            try {
                lemmaRepository.saveAll(lemmaBuffer);
                indexesRepository.saveAll(indexBuffer);
            }catch (Exception ex) {
                log.error("{} \n{} \n{}", ex.getClass().getSimpleName(),ex.getMessage(), ex.getStackTrace());
//                throw new RuntimeException();
            } finally {
                lemmaBuffer.clear();
                indexBuffer.clear();
            }
        }
    }

    synchronized void saveData(Lemma lemma, Indexes index) {
        lemmaBuffer.add(lemma);
        indexBuffer.add(index);
        save();
    }

    public synchronized void saveRemainedLemmasInDB() {
        if(!lemmaBuffer.isEmpty()) lemmaRepository.saveAllAndFlush(lemmaBuffer);
        if(!indexBuffer.isEmpty()) indexesRepository.saveAllAndFlush(indexBuffer);
        clearBuffers();
    }

    public synchronized void clearBuffers() {
        lemmaBuffer.clear();
        indexBuffer.clear();
    }

    synchronized Optional<Lemma> checkBuffer(String key) {
        return lemmaBuffer.stream().filter(l -> l.getLemma().equals(key)).findFirst();
    }
}
