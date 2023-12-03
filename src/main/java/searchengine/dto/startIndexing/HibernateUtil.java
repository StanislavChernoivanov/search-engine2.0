package searchengine.dto.startIndexing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;


@Slf4j
public class HibernateUtil {

    @Getter
    private static final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure("hibernate.cfg.xml").build();
    private static final Metadata METADATA = new MetadataSources(registry).getMetadataBuilder().build();
    @Getter
    private static final SessionFactory SESSION_FACTORY = METADATA.getSessionFactoryBuilder().build();


    static boolean urlIsUnique(String path, Session session) {
         List<Page> resultList;
         CriteriaBuilder builder = session.getCriteriaBuilder();
         CriteriaQuery<Page> query = builder.createQuery(Page.class);
         Root<Page> root = query.from(Page.class);
         query.select(root).where(builder.equal(root.<String>get("path"), path));
         resultList = session.createQuery(query).getResultList();
         return resultList.isEmpty();
    }
}
