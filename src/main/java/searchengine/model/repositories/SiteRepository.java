package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query("select s from Site s where s.url = :url")
    Optional<Site> findSiteByUrl(@Param("url") String url);

    @Query(value = "SELECT count(*) FROM sites", nativeQuery = true)
    int getCountSites();

}
