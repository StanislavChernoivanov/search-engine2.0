package searchengine;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;


@SpringBootApplication
@Log4j2
public class Application {

//    static String text = "Meet my family There are five of us my parents my elder brother my baby sister and me First meet my mum and dad Jane and Michael My mum enjoys reading and my dad enjoys playing chess with my brother Ken My mum is slim and rather tall She has long red hair and big brown eyes She has a very pleasant smile and a soft voice";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
//    @PostConstruct
//    public static void test() throws IOException {
//        LuceneMorphology morphology = new EnglishLuceneMorphology();
//        String [] words = text.split( " ");
//        Arrays.stream(words).toList().forEach(w -> System.out.printf("%s - %s%n", w, morphology.getMorphInfo(w.toLowerCase(Locale.ROOT))));
//    }

}


