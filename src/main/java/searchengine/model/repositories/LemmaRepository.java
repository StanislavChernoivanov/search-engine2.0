package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Lemma;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("select l from Lemma l where l.lemma = :lemma and l.site.id = :id")
    Lemma findLemma(@Param("lemma") String lemma, @Param("id") Integer siteId);

    @Query(value = "SELECT count(*) FROM lemmas", nativeQuery = true)
    int getCountLemmas();

    @Query("select count(l) from Lemma l where l.site.id = :id")
    int findCountLemmasBySiteId(@Param("id") int id);

    @Query("select l from Lemma l where l.site.id = :id and l.lemma = :lemma")
    Lemma findLemmaBySiteIdAndLemma(@Param("id") int id, @Param("lemma") String lemma);
}
