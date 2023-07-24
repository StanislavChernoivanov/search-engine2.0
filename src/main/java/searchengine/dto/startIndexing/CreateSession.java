package searchengine.dto.startIndexing;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;


public class CreateSession {

    private static final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure("hibernate.cfg.xml").build();
    private static final Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
    private static final SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
    public static Session getSession() {
            return sessionFactory.openSession();
    }
}
