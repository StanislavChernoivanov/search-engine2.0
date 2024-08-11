package searchengine.utils.startIndexing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.model.entities.Site;

import java.util.concurrent.ForkJoinPool;


@Log4j2
@NoArgsConstructor
@Component
@Lazy
@Scope("prototype")
public class SiteIndexer extends Thread {
    @Getter
    private ForkJoinPool pool;
    private PageContainer pageContainer;

    private SiteParser siteParser;


    @Override
    public void run() {
        pool = new ForkJoinPool();
        try {
            pool.invoke(siteParser);
        } catch (Exception e) {
            log.error("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        } finally {
            pageContainer.savePages();
            pageContainer.clear();
        }

    }
    @Autowired
    public void setFields(SiteParser siteParser, PageContainer pageContainer) {
        this.siteParser = siteParser;
        this.pageContainer = pageContainer;
    }


}

