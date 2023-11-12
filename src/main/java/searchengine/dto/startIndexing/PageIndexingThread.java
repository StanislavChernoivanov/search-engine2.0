package searchengine.dto.startIndexing;
import searchengine.model.entities.Site;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
public class PageIndexingThread implements Callable<String> {
    private final String url;
    private Site site;

    public PageIndexingThread(String url , Site site) {
        this.site = site;
        this.url = url;
    }

    @Override
    public String call() {
        SiteParser parser;
        try {
            parser = new SiteParser(new URL(url), site);
            SiteNode main = new ForkJoinPool().invoke(parser);
            return ("\n" + main);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
