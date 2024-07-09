package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
@Slf4j
public class SiteParser extends RecursiveAction {

    private final PageContainer pageContainer;

    @Getter
    private final URL url;
    private final Site site;

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final String userAgent;
    private final String referrer;


    public SiteParser(PageContainer pageContainer, URL url, Site site, PageRepository pageRepository
            , SiteRepository siteRepository, String userAgent, String referrer) {
        this.pageContainer = pageContainer;
        this.site = site;
        this.pageRepository = pageRepository;
        this.url = url;
        this.siteRepository = siteRepository;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    private Set<String> getChildes(URL parent) {
        Set<String> childes = new TreeSet<>();
        try {
            if (pageContainer.isContainsPage(parent.getPath().trim())) return childes;
            Connection connection = Jsoup.connect(parent.toString())
                    .maxBodySize(0).timeout(0).ignoreContentType(true);
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
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("thread - {} was interrupted", Thread.currentThread().getName());
        } catch (Exception e) {
            log.warn("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        }
        return childes;
    }

    @Override
    protected void compute() {
        Set<String> childes = getChildes(url);
        if (!childes.isEmpty()) {
            try {
                List<SiteParser> taskList = new ArrayList<>();
                for (String child : childes) {
                    SiteParser task = new SiteParser(pageContainer, new URI(child).toURL(),
                            site, pageRepository, siteRepository, userAgent, referrer);
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
}