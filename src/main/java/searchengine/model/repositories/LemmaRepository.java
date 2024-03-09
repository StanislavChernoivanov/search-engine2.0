package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("select l from Lemma l where l.lemma = :lemma and l.site.id = :id")
    Lemma findLemma(@Param("lemma") String lemma, @Param("id") Integer siteId);

    @Query("select l from Lemma l where l.lemma = :lemma")
    Lemma findLemma(@Param("lemma") String lemma);

    @Query(value = "SELECT count(*) FROM lemmas", nativeQuery = true)
    int getCountLemmas();

    @Query("select count(l) from Lemma l where l.site.id = :id")
    int findCountLemmasBySiteId(@Param("id") int id);

    @Query("select l from Lemma l where l.site.id = :id")
    List<Lemma> findLemmasBySiteId(@Param("id") int id);

    @Query("select l from Lemma l where l.site.id = :id and l.lemma = :lemma")
    Lemma findLemmaBySiteIdAndByLemma(@Param("id") int id, @Param("lemma") String lemma);

    @Query("SELECT CASE WHEN (COUNT(l) > 0) THEN true " +
            "ELSE false END FROM Lemma l WHERE l.lemma = :lemma and l.site.id = :id")
    boolean lemmaIsExist(@Param("lemma") String lemma, @Param("id") Integer siteId);

}
