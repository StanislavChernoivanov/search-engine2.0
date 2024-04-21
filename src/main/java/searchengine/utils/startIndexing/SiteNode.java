package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@Log4j2
public class SiteNode implements Comparable<SiteNode> {

    private final Set<SiteNode> childNodes = new TreeSet<>();

    private final URL url;
    private final PageRepository pageRepository;
    private final Site site;
    private static final Set<Page> pageBuffer = new CopyOnWriteArraySet<>();
    private static volatile Connection connection;
    private final SiteRepository siteRepository;

    public SiteNode(URL url, PageRepository pageRepository, Site site, SiteRepository siteRepository) throws IOException {

        this.pageRepository = pageRepository;
        this.site = site;
        this.url = url;
        this.siteRepository = siteRepository;
        createAndSavePage();
    }

    static {
        connection = Jsoup.newSession().userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                " 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").ignoreContentType(true).maxBodySize(0);
    }

    public synchronized void createAndSavePage() {

        Connection.Response response;
        Document doc = null;
        Page page = new Page();
        try {
            synchronized (connection) {
                connection = Jsoup.connect(url.toString());
                response = connection.execute();
                doc = response.parse();
                page.setCode(response.statusCode());
            }
        }
        catch (HttpStatusException ex) {
            log.info("{}\n{}", ex.getMessage(), ex.getStackTrace());
        }
        catch (IOException exception) {
            log.error("{}\n{}", exception.getMessage(), exception.getStackTrace());
        }
        if (doc != null) {
            page.setContent(doc.html());
        } else {
            page.setContent("");
            page.setCode(404);
        }
        page.setSite(site);
        page.setPath(url.getPath());
        synchronized (pageRepository) {
            if (!pageRepository.pageIsExist(url.getPath(),
                    site.getId())) {
                pageBuffer.add(page);
                if (pageBuffer.size() >= 30) {
                    site.setStatusTime(LocalDateTime.now());
                    synchronized (siteRepository) {
                        siteRepository.save(site);
                    }
                    pageRepository.saveAll(pageBuffer);
                    pageBuffer.clear();
                }
            }
        }
    }

    public static synchronized Set<Page> getRemainingPages() {
        return pageBuffer;
    }

    public void addChild(SiteNode child) {
        synchronized (childNodes) {
            childNodes.add(child);
        }
    }


    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(url.toString());
        if (!childNodes.isEmpty()) {
            childNodes.forEach(c -> buffer.append(System.lineSeparator()).append(c));
        }
        return buffer.toString();
    }

    @Override
    public int compareTo(SiteNode o) {
        return url.toString().compareTo(o.url.toString());
    }
}

