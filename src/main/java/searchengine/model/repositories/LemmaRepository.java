package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Lemma;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("select l from Lemma l where l.lemma = :lemma")
    Lemma findLemma(@Param("lemma") String lemma);

    @Query("SELECT CASE WHEN (COUNT(l) > 0) THEN true ELSE false END FROM Lemma l WHERE l.lemma = :lemma")
    boolean lemmaIsExist(@Param("lemma") String lemma);

    @Override
    <S extends Lemma> S save(S entity);
}
