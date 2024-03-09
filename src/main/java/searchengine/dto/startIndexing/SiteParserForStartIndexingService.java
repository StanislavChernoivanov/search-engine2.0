package searchengine.dto.startIndexing;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class SiteParserForStartIndexingService extends RecursiveTask<SiteNode> {

    private static final Logger LOGGER = LogManager.getLogger(SiteParserForStartIndexingService.class);
    @Getter
    private final URL url;
    private final String host;

    private final Site site;
    private final PageRepository pageRepository;


    public SiteParserForStartIndexingService(URL url, Site site, PageRepository pageRepository) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.url = url;
        host = url.getHost().replaceAll("www\\.", "");
    }

    private Set<String> getChildes(URL parent) {
        Set<String> childes = new TreeSet<>();
        Connection connection = Jsoup.connect(parent.toString()).maxBodySize(0);
        try {
            Thread.sleep((long) (Math.random() * 50 + 100));
            Document doc = connection.get();
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String attr = element.attr("abs:href");
                if (attr.matches(".*#$") || attr.contains("javascript")
                    || !attr.contains(url.toString())
                    || attr.equals(url.toString()))
                    continue;
                childListAdd(childes, attr);
            }
        } catch (HttpStatusException ignored) {}
        catch (Exception e) {
            LOGGER.error("{}\n{}", e.getMessage(), e.getStackTrace());
        }
        return childes;
    }

    @Override
    protected SiteNode compute() {
        Set<String> childes = getChildes(url);
        if (childes.isEmpty()) {
            try {
                return new SiteNode(url, pageRepository, site);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            SiteNode node = null;
            try {
                node = new SiteNode(url, pageRepository, site);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                List<SiteParserForStartIndexingService> taskList = getSiteParserForStartIndexingServices(childes);
                for (SiteParserForStartIndexingService task : taskList) {
                        SiteNode child = task.join();
                        node.addChild(child);
                }
            } catch (Exception exception) {
//                exception.printStackTrace();
                LOGGER.error("{} \n{}", exception.getMessage(), exception.getStackTrace());
            }
            return node;
        }
    }

    private List<SiteParserForStartIndexingService> getSiteParserForStartIndexingServices(Set<String> childes) {
        List<SiteParserForStartIndexingService> taskList = new ArrayList<>();
        for (String child : childes) {
            SiteParserForStartIndexingService task = null;
            try {
                task = new SiteParserForStartIndexingService
                                (new URL(child), site, pageRepository);
            } catch (MalformedURLException ignored) {}
            try {
                task.fork();
                taskList.add(task);
            } catch (NullPointerException ignored) {
            }
        }
        return taskList;
    }

    private void childListAdd(Set<String> childes, String child) throws MalformedURLException {
        Optional<String> isChild = childes.stream().filter(child::contains).findFirst();
        if (isChild.isEmpty()) {
            try {
                URL childURL = new URL(child);
                if (!childURL.getHost().replaceAll("www\\.", "").equals(host)) {
                    return;
                }
            } catch (MalformedURLException ignored) {
            }
            if (!childes.isEmpty()) {
                childes.removeIf(c -> c.contains(child));
            }
            childes.add(child);
        }
    }
}