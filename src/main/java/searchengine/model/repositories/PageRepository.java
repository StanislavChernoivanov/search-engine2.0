package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query("select p from Page p where p.path = :path")
    Page findPageWithPath(@Param("path") String path);
}
