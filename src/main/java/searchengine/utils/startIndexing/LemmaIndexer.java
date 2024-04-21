package searchengine.utils.startIndexing;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.entities.Page;
import java.util.*;

@RequiredArgsConstructor
@Log4j2
public class LemmaIndexer implements Runnable {
    @Getter
    private final Page page;
    private final SaverOrRefresher saverOrRefresher;


    public void run() {
        Map<String, Integer> lemmas = new LemmaCollector()
                .collectLemmas(clearTags(page.getContent()));
        lemmas.keySet().forEach(k -> insertInDataBase(k, lemmas.get(k)));
    }

    private void insertInDataBase
            (String key, Integer rank) {
        saverOrRefresher.saveOrRefresh(key, rank, page);
    }

    public String clearTags(String content) {
        StringBuilder builder = new StringBuilder();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.getAllElements();
        elements.forEach(e -> builder.append(e.ownText()).append(" "));
        return builder.toString();
    }
}

