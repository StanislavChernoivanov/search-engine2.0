package searchengine.dto.startIndexing;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<SiteNode> {

    private static final Logger LOGGER = LogManager.getLogger(SiteParser.class);
    private final Site site;

    @Getter
    private final URL url;
    private final String host;

    public SiteParser(URL url, Site site) {
        this.url = url;
        this.site = site;
        host = url.getHost().replaceAll("www\\.", "");
    }

    private Set<String> getChildes(URL parent) {
        String path = "";
        Set<String> childes = new TreeSet<>();
        Connection connection = Jsoup.connect(parent.toString());
        try {
            Document doc = connection.
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").ignoreContentType(true).get();
            Thread.sleep((long) (Math.random() * 700) + 800);
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String attr = element.attr("abs:href");
                if (attr.contains("http") || attr.contains("https")) {
                    URL attrUrl = new URL(attr);
                    if (!attrUrl.getHost().replaceAll("www\\.", "").equals(host) ||
                            attr.matches(".*#$") || attr.contains("javascript") ||
                            !attr.contains(url.toString()) || attr.equals(url.toString()) ||
                            attrUrl.getPath().equals("/") ||
                            !CreateSession.urlIsUnique(new URL(attr).getPath())) continue;
                    Session session = CreateSession.getSession();
                    Transaction transaction = session.beginTransaction();
                    try{
                        Page page = new Page();
                        page.setCode(connection.response().statusCode());
                        page.setContent(doc.html());
                        page.setSite(site);
                        page.setPath(attrUrl.getPath());
                        session.save(page);
                        transaction.commit();
                    } catch (Exception e) {
                        transaction.rollback();
                    } finally {
                        session.close();
                    }
                    childListAdd(childes, attr);
                    path = attr;
                }
            }
        } catch (Exception e) {
            LOGGER.error("{} \n {} \n{}",path, e.getMessage(), e.getStackTrace());
        }
        return childes;
    }

    @Override
    protected SiteNode compute() {
        Set<String> childes = getChildes(url);
        if (childes.size() == 0) {
            return new SiteNode(url);
        } else {
            SiteNode node = new SiteNode(url);
            try {
                List<SiteParser> taskList = new ArrayList<>();
                for (String child : childes) {
                    URL childURL = new URL(child);
                    SiteParser task = new SiteParser(childURL, site);
                    task.fork();
                    taskList.add(task);
                }
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

    private void childListAdd(Set<String> childes, String child) {
        Optional<String> isChild = childes.stream().filter(child::contains).findFirst();
        if (isChild.isEmpty()) {
            try {
                URL childURL = new URL(child);
                if (!childURL.getHost().replaceAll("www\\.", "").equals(host)) {
                    return;
                }
            } catch (Exception exception) {
                LOGGER.error("{} \n {} \n{}",child, exception.getMessage(), exception.getStackTrace());
            }
            if (childes.size() > 0) {
                childes.removeIf(c -> c.contains(child));
            }
            childes.add(child);
        }
    }
}