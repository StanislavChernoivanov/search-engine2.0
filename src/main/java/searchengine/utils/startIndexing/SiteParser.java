package searchengine.utils.startIndexing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.utils.ObjectFactoryHolder;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Component
@Scope("prototype")
@NoArgsConstructor
@Lazy
public class SiteParser extends RecursiveAction {
    @Autowired
    private PageContainer pageContainer;

    @Getter
    private URL url;
    private Site site;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    private String userAgent;
    private String referrer;
    @Autowired
    private ObjectFactoryHolder objectFactoryHolder;

    public void setFields(URL url,
                          Site site,
                          PageRepository pageRepository,
                          SiteRepository siteRepository,
                          String userAgent,
                          String referrer,
                          PageContainer pageContainer,
                          ObjectFactoryHolder objectFactoryHolder) {
        this.url = url;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.pageContainer = pageContainer;
        this.objectFactoryHolder = objectFactoryHolder;
    }

    @Override
    protected void compute() {
        Set<String> childes = getChildes(url);
        if (!childes.isEmpty()) {
            try {
                List<SiteParser> taskList = new ArrayList<>();
                for (String child : childes) {
                    SiteParser task = objectFactoryHolder.getSiteParser(
                            new URI(child).toURL(),
                            site,
                            pageRepository,
                            siteRepository,
                            userAgent,
                            referrer,
                            pageContainer,
                            objectFactoryHolder);
                    task.fork();
                    taskList.add(task);
                }
                taskList.forEach(ForkJoinTask::join);
            } catch (CancellationException exception) {
                log.debug("task - {} was cancelled", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
            }
        }
    }

    private Set<String> getChildes(URL parent) {
        Set<String> childes = new TreeSet<>();
        try {
            if (pageContainer.isContainsPage(parent.getPath().trim())) return childes;
            Connection connection = Jsoup.connect(parent.toString()).
                    referrer(referrer).
                    userAgent(userAgent).
                    maxBodySize(0).
                    timeout(0).
                    ignoreContentType(true);
            Document doc;
            Thread.sleep((long) (Math.random() * 50 + 100));
            try {
                doc = connection.get();
            } catch (SocketException | HttpStatusException e) {
                pageContainer.addPage(parent, null, site);
                return childes;
            }
            pageContainer.addPage(parent, doc, site);
            Elements elements = doc.select("a[href]");

            for (Element element : elements) {
                String attr = element.attr("abs:href");
                try {
                    URL newURL = new URI(attr).toURL();
                    if (attr.matches(".*#.*$")
                            || attr.contains("javascript")
                            || attr.toLowerCase()
                            .matches("^.+\\.jpg|.+\\.jpeg|.+\\.png|.+\\.pdf$")
                            || attr.contains(" ")
                            || newURL.getPath().equals(parent.getPath())
                            || pageContainer.isContainsPage(newURL.getPath())
                            || !parent.getHost().replaceAll("www\\.", "")
                            .equals(newURL.getHost()
                                    .replaceAll("www\\.", ""))
                    ) continue;

                    childes.add(attr);
                } catch (MalformedURLException | URISyntaxException ignored) {
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("thread - {} was interrupted", Thread.currentThread().getName());
        } catch (Exception e) {
            log.warn("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        }
        return childes;
    }

}