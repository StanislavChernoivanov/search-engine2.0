package searchengine.model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Override
    @Modifying
    <S extends Site> S save(S entity);
}
