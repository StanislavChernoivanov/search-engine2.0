package searchengine.model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Transactional()
    @Override
    <S extends Page> S save(S entity);
}
