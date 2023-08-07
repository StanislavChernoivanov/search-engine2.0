package searchengine.dto.startIndexing;

import searchengine.model.Site;

import java.net.MalformedURLException;
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
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        SiteNode main = new ForkJoinPool().invoke(parser);
            return (System.lineSeparator() + main);
    }
}
