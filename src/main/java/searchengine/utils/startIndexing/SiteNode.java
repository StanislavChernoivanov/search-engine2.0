package searchengine.utils.startIndexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.io.IOException;
import java.net.SocketTimeoutException;
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
    private static Set<Page> pageBuffer;
    private Connection connection;
    private final SiteRepository siteRepository;
    private final String userAgent;
    private final String referrer;

    static {
        pageBuffer = new CopyOnWriteArraySet<>();
    }

    public SiteNode(URL url,
                    PageRepository pageRepository,
                    Site site,
                    SiteRepository siteRepository, String referrer, String userAgent) throws IOException {

        this.pageRepository = pageRepository;
        this.site = site;
        this.url = url;
        this.siteRepository = siteRepository;
        this.userAgent = userAgent;
        this.referrer = referrer;
        createAndSavePage();
    }

    public synchronized void createAndSavePage() {
        Connection.Response response;
        Document doc = null;
        Page page = new Page();
        try {
            connection = Jsoup.connect(url.toString())
                    .userAgent(userAgent).referrer(referrer)
                    .ignoreContentType(true).maxBodySize(0).timeout(20_000);
            response = connection.execute();
            doc = response.parse();
            page.setCode(response.statusCode());
        } catch (SocketTimeoutException socketTimeoutEx) {
            log.info("{} - {}", url.toString(), socketTimeoutEx.getMessage());
        } catch (IOException ignored) {}
        synchronized (pageRepository) {
            if (doc != null) {
                page.setContent(doc.html());
            } else {
                page.setContent("");
                page.setCode(404);
            }
            page.setSite(site);
            page.setPath(url.getPath());
            if (!pageRepository.pageIsExist(url.getPath(),
                    site.getId())) {
                pageBuffer.add(page);
                if (pageBuffer.size() >= 30) {
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    pageRepository.saveAll(pageBuffer);
                    pageBuffer.clear();
                }
            }
        }
    }

    public static synchronized Set<Page> getPagesBuffer() {
        return pageBuffer;
    }
    public static synchronized void clearPagesBuffer() {
        pageBuffer.clear();
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


