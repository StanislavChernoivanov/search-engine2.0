package searchengine;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;


@SpringBootApplication
public class Application {
    public Application() {
    }

    public static void main(String[] args) throws IOException {
//        printLemma();
        SpringApplication.run(Application.class, args);
    }
//    @PostConstruct
//    public static void printLemma() throws IOException {
//        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
//        List<String> list = luceneMorphology.getMorphInfo("городу");
//        list.forEach(System.out::print);
//    }

}
