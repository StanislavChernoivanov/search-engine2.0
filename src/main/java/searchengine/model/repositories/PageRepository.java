package searchengine.model.repositories;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Page;
import java.util.List;

@Repository
public interface PageRepository extends PagingAndSortingRepository<Page, Integer> {

    @Query("select p from Page p where p.path = :path")
    Page findPageByPath(@Param("path") String path);

    @Query("select p.path from Page p where p.path in (:path)")
    List<String> findPageByManyPaths(@Param("path") List<String> paths);


    @Query(value = "SELECT count(*) FROM pages", nativeQuery = true)
    int getCountPages();


    @Query("select count(p) from Page p where p.site.id = :id")
    int findCountPagesBySiteId(@Param("id") int id);
    @Query("select i.page from Indexes i where i.lemma.id = :lemmaId")
    List<Page> findPagesByLemma(@Param("lemmaId") int lemmaId);

    @Query("select i.page from Indexes i where i.lemma.id in (:lemmaId)")
    List<Page> findPagesByLemmaList(@Param("lemmaId") List<Integer> lemmaIdList);

    @Query("SELECT CASE WHEN (COUNT(p) > 0) " +
            "THEN true ELSE false END FROM Page p WHERE p.path = :path and p.site.id = :id")
    boolean pageIsExist(@Param("path") String path, @Param("id") Integer siteId);


    @Query("select p from Page p where p.site.id = :id")
    org.springframework.data.domain.Page<Page> findPageBySiteId(@Param("id") int id, Pageable pageable);
}
