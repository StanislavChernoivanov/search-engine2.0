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

}
