package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

}
