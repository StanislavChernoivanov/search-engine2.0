package searchengine.dto.startIndexing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import searchengine.model.Site;

import java.net.URL;
import java.util.concurrent.ForkJoinPool;
public class PageIndexingThread extends Thread {
    private final String url;
    public static final Logger LOGGER = LogManager.getLogger(PageIndexingThread.class);
    private Site site;

    public PageIndexingThread(String url , Site site) {
        this.site = site;
        this.url = url;
    }

    @Override
    public void run() {
        try {
            SiteParser parser = new SiteParser(new URL(url), site);
            SiteNode main = new ForkJoinPool().invoke(parser);
            LOGGER.info(System.lineSeparator() + main);
        } catch (Exception exception) {
            LOGGER.error("{}: {}", exception.getMessage(), exception.getStackTrace());
        }
    }
}
