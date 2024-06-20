package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;


@RequiredArgsConstructor
@Log4j2
public class SiteIndexer extends Thread {
    private final String urlStr;
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
        URL url = null;
        try {
            url = new URI(urlStr).toURL();
            parser = new SiteParser(url,
                    site, repository, siteRepository, userAgent, referrer);
            pool.invoke(parser);
        } catch (Exception e) {
            log.error("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        } finally {
            assert url != null;
            SiteParser.clearBuffer();
        }
    }



}

