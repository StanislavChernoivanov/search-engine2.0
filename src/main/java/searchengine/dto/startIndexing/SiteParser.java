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
import java.net.MalformedURLException;
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
                    .referrer("http://www.google.com").ignoreContentType(true).ignoreHttpErrors(true).get();
            Elements elements = doc.select("a[href]");
            Session session = HibernateUtil.getSESSION_FACTORY().openSession();
            String attr = null;
            for (Element element : elements) {
                attr = element.attr("abs:href");
                URL attrUrl = null;
                try {
                    attrUrl = new URL(attr);
                } catch (MalformedURLException ignored) {
                }
                assert attrUrl != null;
                if (!attrUrl.getHost().replaceAll("www\\.", "").equals(host) ||
                        attr.matches(".*#$") || attr.contains("javascript") ||
                        !attr.contains(url.toString()) || attr.equals(url.toString()) ||
                        attrUrl.getPath().equals("/") ||
                        !HibernateUtil.urlIsUnique(attrUrl.getPath(), session)) {
                    continue;
                }
                Connection con = Jsoup.connect(attr);
                Document d = con.
                        userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .ignoreContentType(true).ignoreHttpErrors(true).get();
                Transaction t = session.getTransaction();
                try {
                    t.begin();
                    Page page = new Page();
                    page.setCode(con.response().statusCode());
                    page.setContent(d.html());
                    page.setSite(site);
                    page.setPath(attrUrl.getPath());
                    session.save(page);
                    t.commit();
                } catch (Exception e) {
                    t.rollback();
                    e.printStackTrace();
                }
            }
            assert attr != null;
            childListAdd(childes, attr);
            path = attr;
            session.close();
        } catch (Exception e) {
            LOGGER.error("{} \n {} \n{}",path, e.getMessage(), e.getStackTrace());
            e.printStackTrace();
        }
        return childes;
    }

        @Override
    protected SiteNode compute() {
        Set<String> childes = getChildes(url);
        if (childes.isEmpty()) {
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
        if (isChild.isEmpty()) return;
        childes.removeIf(c -> c.contains(child));
        childes.add(child);
    }
}