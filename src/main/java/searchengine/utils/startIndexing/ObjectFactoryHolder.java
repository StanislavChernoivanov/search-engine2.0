package searchengine.utils.startIndexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.net.URL;

@Component
@RequiredArgsConstructor
@Lazy
public class ObjectFactoryHolder {

    private final org.springframework.beans.factory.ObjectFactory<SiteParser> siteParserObjectFactory;
    private final ObjectFactory<SiteIndexer> siteIndexerObjectFactory;
    private final ObjectFactory<PageContainer> pageContainerObjectFactory;


    public SiteParser getSiteParser(URL url,
                                    Site site,
                                    PageRepository pageRepository,
                                    SiteRepository siteRepository,
                                    String userAgent,
                                    String referrer,
                                    PageContainer pageContainer,
                                    ObjectFactoryHolder objectFactoryHolder) {
        SiteParser siteParser = siteParserObjectFactory.getObject();
        siteParser.setFields(url,
                site,
                pageRepository,
                siteRepository,
                userAgent,
                referrer,
                pageContainer,
                objectFactoryHolder);
        return siteParser;
    }

    public SiteIndexer getSiteIndexer(SiteParser siteParser, PageContainer container) {
        SiteIndexer siteIndexer = siteIndexerObjectFactory.getObject();
        siteIndexer.setFields(siteParser, container);
        return siteIndexer;
    }

    public PageContainer getPageContainer(PageRepository pageRepository) {
        PageContainer pageContainer = pageContainerObjectFactory.getObject();
        pageContainer.setPageRepository(pageRepository);
        return pageContainer;
    }



}
