package searchengine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.config.SitesList;


@SpringBootApplication
public class Application {

    @Autowired
    private SitesList sites;
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
//    @PostConstruct
//    public void init() {
//        StartIndexingServiceImpl startIndexingService = new StartIndexingServiceImpl(sites);
//        startIndexingService.startIndexing();
//    }
}
