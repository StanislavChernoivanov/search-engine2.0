package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;


@RequiredArgsConstructor
@Log4j2
public class SiteIndexer extends Thread {
    private final String url;
    @Getter
    private final Site site;
    private final PageRepository repository;
    private final SiteRepository siteRepository;
    @Getter
    private ForkJoinPool pool;

    private final String userAgent;
    private final String referrer;

    @Override
    public void run() {
        SiteParser parser;
        pool = new ForkJoinPool();
        try {
            parser = new SiteParser(new URL(url), site, repository, siteRepository, userAgent, referrer);
            pool.invoke(parser);
        } catch (MalformedURLException e) {
            log.error("{}\n{}", e.getMessage(), e.getStackTrace());
        }
    }
}

