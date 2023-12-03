package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Site;
import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query("select s from Site s where s.status = :status")
    List<Site> findSiteByStatus(@Param("status") String status);
}
