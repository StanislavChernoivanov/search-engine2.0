package searchengine.dto.startIndexing;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;


@Slf4j
public class CreateSession {

    private static final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure("hibernate.cfg.xml").build();
    private static final Metadata METADATA = new MetadataSources(registry).getMetadataBuilder().build();
    private static final SessionFactory SESSION_FACTORY = METADATA.getSessionFactoryBuilder().build();
     static Session getSession() {
            return SESSION_FACTORY.openSession();
    }

    static boolean urlIsUnique(String path, Session session) {
         List<Page> resultList;
         CriteriaBuilder builder = session.getCriteriaBuilder();
         CriteriaQuery<Page> query = builder.createQuery(Page.class);
         Root<Page> root = query.from(Page.class);
         query.select(root).where(builder.equal(root.<String>get("path"), path));
         resultList = session.createQuery(query).getResultList();
         return resultList.isEmpty();
    }

    static Lemma findLemma(String lemma, Session session) {
    if(getResultList(lemma, session).isEmpty()) throw new NullPointerException("Lemma is not found!!!!");
    return getResultList(lemma, session).get(0);
    }
    static boolean lemmaIsExist(String lemma, Session session) {
         List<Lemma> resultList = getResultList(lemma, session);
         return !resultList.isEmpty();
    }

    static List<Lemma> getResultList(String lemma, Session session) {
        List<Lemma> result;
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Lemma> query = builder.createQuery(Lemma.class);
        Root<Lemma> root = query.from(Lemma.class);
        query.select(root).where(builder.equal(root.<String>get("lemma"), lemma));
        result = session.createQuery(query).getResultList();
        return result;
    }


    static boolean indexIsExist(int lemmaId, int pageId) {
         Session session = SESSION_FACTORY.openSession();
        String hql = "From " + Indexes.class.getSimpleName() +
                " Where lemma_id = " + lemmaId + " And page_id = " + pageId;
            List resultList = session.createQuery(hql).getResultList();
            session.close();
        return !resultList.isEmpty();
    }
}
