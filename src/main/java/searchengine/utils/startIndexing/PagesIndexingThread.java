package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;


@RequiredArgsConstructor
@Log4j2
public class PagesIndexingThread implements Runnable {
    private final String url;
    @Getter
    private final Site site;
    private final PageRepository repository;

    @Override
    public void run() {
        SiteParserForStartIndexingService parser;
        ForkJoinPool pool = new ForkJoinPool();
        try (pool) {
            parser = new SiteParserForStartIndexingService(new URL(url), site, repository);
            SiteNode main = pool.invoke(parser);
        } catch (MalformedURLException e) {
            log.error("{}\n{}", e.getMessage(), e.getStackTrace());
        }
    }
}

