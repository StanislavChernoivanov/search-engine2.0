package searchengine.dto.startIndexing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
@RequiredArgsConstructor
public class PageIndexingThread implements Runnable, Comparable<PageIndexingThread> {
    private final String url;
    @Getter
    private final Site site;
    private final PageRepository repository;

    @Override
    public void run() {
        SiteParser parser;
        try {
            parser = new SiteParser(new URL(url), site, repository);
            SiteNode main = new ForkJoinPool().invoke(parser);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int hashCode() {
        return this.site.getId();
    }

    @Override
    public int compareTo(PageIndexingThread o) {
        return this.site.getName().compareTo(o.getSite().getName());
    }
}
