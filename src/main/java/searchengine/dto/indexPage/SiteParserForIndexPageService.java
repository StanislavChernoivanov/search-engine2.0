package searchengine.dto.indexPage;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.startIndexing.SiteNode;
import searchengine.dto.startIndexing.SiteParserForStartIndexingService;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class SiteParserForIndexPageService extends RecursiveTask<SiteNode> {
    @Override
    protected SiteNode compute() {
        return null;
    }
//    private static final Logger LOGGER = LogManager.getLogger(SiteParserForStartIndexingService.class);
//    private final PageRepository pageRepository;
//    private final Site site;
//
//    @Getter
//    private final URL url;
//    private final String host;
//
//    public SiteParserForIndexPageService(URL url, Site site, PageRepository pageRepository) {
//        this.pageRepository = pageRepository;
//        this.url = url;
//        this.site = site;
//        host = url.getHost().replaceAll("www\\.", "");
//    }
//
//    private Set<String> getChildes(URL parent) {
//        String path = "";
//        Set<String> childes = new TreeSet<>();
//        Connection connection = Jsoup.connect(parent.toString());
//        try {
//            Document doc = connection.
//                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                    .referrer("http://www.google.com").ignoreContentType(true).ignoreHttpErrors(true).get();
//            Elements elements = doc.select("a[href]");
//            String attr = null;
//            for (Element element : elements) {
//                attr = element.attr("abs:href");
//                URL attrUrl = null;
//                try {
//                    attrUrl = new URL(attr);
//                } catch (MalformedURLException ignored) {}
//                if(attrUrl == null) continue;
//                if (!attrUrl.getHost().replaceAll("www\\.", "").equals(host) ||
//                        attr.matches(".*#$") || attr.contains("javascript") ||
//                        !attr.contains(url.toString()) || attr.equals(url.toString()) ||
//                        attrUrl.getPath().equals("/") ||
//                        pageRepository.pageIsExist(attrUrl.getPath())) {
//                    continue;
//                }
//                Connection con = Jsoup.connect(attr);
//                Document d = con.
//                        userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                        .referrer("http://www.google.com")
//                        .ignoreContentType(true).ignoreHttpErrors(true).get();
//                Page page = new Page();
//                page.setCode(con.response().statusCode());
//                page.setContent(d.html());
//                page.setSite(site);
//                page.setPath(attrUrl.getPath());
//                pageRepository.save(page);
//            }
//            assert attr != null;
//            childListAdd(childes, attr);
//        } catch (Exception e) {
//            LOGGER.error("{} \n {} \n{}",path, e.getMessage(), e.getStackTrace());
//        }
//        return childes;
//    }
//
//    @Override
//    protected SiteNode compute() {
//        Set<String> childes = getChildes(url);
//        if (childes.isEmpty()) {
//            return new SiteNode(1, url);
//            // TODO: 10.01.2024 level 1 is random
//        } else {
//            SiteNode node = new SiteNode(1, url);
//            // TODO: 10.01.2024 level 1 is random
//            try {
//                List<SiteParserForStartIndexingService> taskList = new ArrayList<>();
//                for (String child : childes) {
//                    URL childURL = new URL(child);
//                    SiteParserForStartIndexingService task =
//                            new SiteParserForStartIndexingService(childURL, site, pageRepository, 1);
//                    // TODO: 10.01.2024 level 1 is random
//                    task.fork();
//                    taskList.add(task);
//                }
//                for (SiteParserForStartIndexingService task : taskList) {
//                    SiteNode child = task.join();
//                    node.addChild(child);
//                }
//            } catch (Exception exception) {
//                LOGGER.error("{} \n{}", exception.getMessage(), exception.getStackTrace());
//            }
//            return node;
//        }
//    }
//
//    private void childListAdd(Set<String> childes, String child) {
//        Optional<String> isChild = childes.stream().filter(child::contains).findFirst();
//        if (isChild.isEmpty()) return;
//        childes.removeIf(c -> c.contains(child));
//        childes.add(child);
//    }


}
