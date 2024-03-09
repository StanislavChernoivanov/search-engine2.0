package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;

import java.util.List;

@Repository
public interface IndexesRepository extends JpaRepository<Indexes, Integer> {
    @Query("select case when (count(i) > 0) then true else false end " +
            "from Indexes i where i.lemma.id = :lemma and i.page.id = :page")
    boolean indexIsExist(@Param(value = "lemma") int lemma, @Param("page") int page);


    @Query(value = "SELECT count(*) FROM indexes", nativeQuery = true)
    int getCountIndexes();


    @Query("select i from Indexes i where i.page.id = :page_id")
    List<Indexes> findIndexWithSpecifiedPageId(@Param("page_id") int pageId);

    @Query("select i from Indexes i where i.lemma.id = :lemma_id")
    List<Indexes> findIndexWithSpecifiedLemmaId(@Param("lemma_id") int lemmaId);
}
