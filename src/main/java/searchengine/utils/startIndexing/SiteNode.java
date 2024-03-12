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
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Log4j2
public class SiteNode implements Comparable<SiteNode> {

    private final Set<SiteNode> childNodes = new TreeSet<>();

    private final URL url;
    private final PageRepository pageRepository;
    private final Site site;


    public SiteNode(URL url, PageRepository pageRepository, Site site) throws IOException {
        this.pageRepository = pageRepository;
        this.site = site;
        this.url = url;

        Connection connection;
        Connection.Response response = null;
        Document doc = null;
        Page page = new Page();
        try {
            connection = Jsoup.connect(url.toString()).
                    maxBodySize(0).timeout(5000);
            response = connection.execute();
            doc = response.parse();
        }
        catch (HttpStatusException ex) {
            log.info("{}\n{}", ex.getMessage(), ex.getStackTrace());
        }
        catch (IOException exception) {
            log.error("{}\n{}", exception.getMessage(), exception.getStackTrace());
        }
        if(response != null) {
            page.setCode(response.statusCode());
        } else page.setCode(404);
        if (doc != null) {
            page.setContent(doc.html());
        } else  page.setContent("");
        page.setSite(site);
        page.setPath(url.getPath());
        synchronized (this) {
            if (!pageRepository.pageIsExist(url.getPath(), site.getId()))
                pageRepository.save(page);
        }
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

