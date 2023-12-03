package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Indexes;

@Repository
public interface IndexesRepository extends JpaRepository<Indexes, Integer> {
    @Query("select case when (count(i) > 0) then true else false end " +
            "from Indexes i where i.lemma = :lemma and i.page = :page")
    boolean indexIsExist(@Param(value = "lemma") int lemma, @Param("page") int page);

}
