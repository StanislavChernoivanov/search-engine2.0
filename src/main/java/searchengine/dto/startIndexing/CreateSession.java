package searchengine.dto.startIndexing;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import searchengine.model.Page;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;



public class CreateSession {

    private static final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure("hibernate.cfg.xml").build();
    private static final Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
    private static final SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
     static Session getSession() {
            return sessionFactory.openSession();
    }

    static synchronized boolean urlIsUnique(String path) {
         Session session = sessionFactory.openSession();
         List<Page> resultList = new ArrayList<>();
        Transaction transaction = session.beginTransaction();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Page> query1 = builder.createQuery(Page.class);
            Root<Page> root = query1.from(Page.class);
            query1.select(root).where(builder.equal(root.<String>get("path"), path));
            resultList = session.createQuery(query1).getResultList();
            transaction.commit();
        }catch (Exception ex) {
            ex.printStackTrace();
            transaction.rollback();
        } finally {
            session.close();
        }
        return resultList.isEmpty();
    }
}
