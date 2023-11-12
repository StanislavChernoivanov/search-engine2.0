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

    public synchronized static Optional<Lemma> findLemma(String lemma, Session session) {
        Optional<List<Lemma>> optionalLemmaList = getResultList(lemma, session);
        if (optionalLemmaList.isPresent() && !optionalLemmaList.get().isEmpty())
            return Optional.ofNullable(optionalLemmaList.get().get(0));
        return Optional.empty();
    }
//    static boolean lemmaIsExist(String lemma, Session session) {
//         Optional<List<Lemma>> resultList = getResultList(lemma, session);
//         return resultList.isPresent();
//    }

     public synchronized static Optional<List<Lemma>> getResultList(String lemma, Session session) {
        Optional<List<Lemma>> result;
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Lemma> query = builder.createQuery(Lemma.class);
        Root<Lemma> root = query.from(Lemma.class);
        query.select(root).where(builder.equal(root.<String>get("lemma"), lemma));
        result = Optional.ofNullable(session.createQuery(query).getResultList());
        if (result.isEmpty()) throw new RuntimeException();
        return result;
    }

    //    static synchronized boolean indexIsExist(int lemmaId, int pageId, Session session) {
//        String hql = "From " + Indexes.class.getSimpleName() +
//                " Where lemma_id = " + lemmaId + " And page_id = " + pageId;
//            List resultList = session.createQuery(hql).getResultList();
//        return !resultList.isEmpty();
//    }

//    public static Page getPage(Session session, String path){
//        List<Page> resultList;
//        CriteriaBuilder builder = session.getCriteriaBuilder();
//        CriteriaQuery<Page> query = builder.createQuery(Page.class);
//        Root<Page> root = query.from(Page.class);
//        query.select(root).where(builder.equal(root.<String>get("path"), path));
//        resultList = session.createQuery(query).getResultList();
//        if(resultList.isEmpty()) log.error("Страница не найдена");
//        return resultList.get(0);
//    }

    public static List<Page> getPageList(int siteId, Session session){
       List<Page> resultList;
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Page> query = builder.createQuery(Page.class);
        Root<Page> root = query.from(Page.class);
        query.select(root).where(builder.equal(root.<String>get("site"), siteId));
        resultList = session.createQuery(query).getResultList();
        if(resultList.isEmpty()) log.error("Страница не найдена");
        return resultList;
    }
}
