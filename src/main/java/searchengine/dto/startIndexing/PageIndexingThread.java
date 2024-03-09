package searchengine.dto.startIndexing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;


@RequiredArgsConstructor
public class PageIndexingThread implements Runnable {
    private final String url;
    @Getter
    private final Site site;
    private final PageRepository repository;

    public static final Logger LOGGER = LogManager.getLogger(PageIndexingThread.class);

    @Override
    public void run() {
        SiteParserForStartIndexingService parser;
        try {
            parser = new SiteParserForStartIndexingService(new URL(url), site, repository);
            SiteNode main = new ForkJoinPool().invoke(parser);
            LOGGER.info(System.lineSeparator() + main);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}

