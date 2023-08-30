package searchengine.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
}
