package searchengine.utils.startIndexing;
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
import searchengine.model.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<SiteNode> {

    private static final Logger LOGGER = LogManager.getLogger(SiteParser.class);
    @Getter
    private final URL url;
    private final String host;
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;


    public SiteParser(URL url, Site site, PageRepository pageRepository, SiteRepository siteRepository) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.url = url;
        host = url.getHost().replaceAll("www\\.", "");
        this.siteRepository = siteRepository;
    }

    private Set<String> getChildes(URL parent) {
        Set<String> childes = new TreeSet<>();
        Connection connection = Jsoup.connect(parent.toString()).userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                        " 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
        try {
            Thread.sleep((long) (Math.random() * 50 + 100));
            Document doc = connection.ignoreContentType(true).get();
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
                return new SiteNode(url, pageRepository, site, siteRepository);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            SiteNode node;
            try {
                node = new SiteNode(url, pageRepository, site, siteRepository);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                List<SiteParser> taskList
                        = getSiteParserForStartIndexingServices(childes);
                for (SiteParser task : taskList) {
                        SiteNode child = task.join();
                        node.addChild(child);
                }
            } catch (Exception exception) {
                LOGGER.error("{} \n{}", exception.getMessage(), exception.getStackTrace());
            }
            return node;
        }
    }

    private List<SiteParser> getSiteParserForStartIndexingServices(Set<String> childes) {
        List<SiteParser> taskList = new ArrayList<>();
        for (String child : childes) {
            SiteParser task = null;
            try {
                task = new SiteParser
                                (new URL(child), site, pageRepository, siteRepository);
            } catch (MalformedURLException ignored) {}
            try {
                task.fork();
                taskList.add(task);
            } catch (NullPointerException ignored) {
            }
        }
        return taskList;
    }

    private void childListAdd(Set<String> childes, String child) {
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