package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Indexes;

import java.util.List;

@Repository
public interface IndexesRepository extends JpaRepository<Indexes, Integer> {
    @Query("select i from Indexes i where i.page.id = :page_id")
    List<Indexes> findIndexWithSpecifiedPageId(@Param("page_id") int pageId);

    @Query("select count(i) from Indexes i where i.page.id = :pageId and i.lemma.id = :lemmaId")
    int findOutNumberOfIndexesWithSpecifiedPageIdAndLemmaId(@Param("pageId") int pageId, @Param("lemmaId") int lemmaId);


    @Query("select i.rank from Indexes i where i.page.id = :pageId and i.lemma.frequency / :pagesAmount * 100 < 30")
    List<Float> findIndexByPageId(@Param("pageId") int pageId, @Param("pagesAmount") int pagesAmount);

}
