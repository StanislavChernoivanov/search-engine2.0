package searchengine.dto.startIndexing;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import searchengine.model.Page;
import java.util.*;

public class CreateSession {

    private static Set<Page> pageSet = new HashSet<>();
    private static final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure("hibernate.cfg.xml").build();
    private static final Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
    private static final SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
    public static Session getSession() {
        return sessionFactory.openSession();
    }


//    public static synchronized void addInPageList(Page page) {
//        pageSet.add(page);
//    }
//    public static Set<Page> getPageList() {
//        return pageSet;
//    }
//
//    public static void clearPageList(){
//        pageSet.clear();
//    }

}
