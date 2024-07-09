package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;


@Log4j2
public class SiteIndexer extends Thread {
    @Getter
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    @Getter
    private ForkJoinPool pool;

    private final String userAgent;
    private final String referrer;

    private final PageContainer pageContainer;

    public SiteIndexer(Site site
            , PageRepository pageRepository
            , SiteRepository siteRepository
            , String userAgent
            , String referrer) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.userAgent = userAgent;
        this.referrer = referrer;
        pageContainer = new PageContainer(pageRepository);
    }

    @Override
    public void run() {
        SiteParser parser;
        pool = new ForkJoinPool();
        URL url;
        try {
            url = new URI(site.getUrl()).toURL();
            parser = new SiteParser(pageContainer, url,
                    site, pageRepository, siteRepository, userAgent, referrer);
            pool.invoke(parser);
        } catch (Exception e) {
            log.error("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        } finally {
            pageContainer.savePages();
            pageContainer.clear();
        }

    }


}

