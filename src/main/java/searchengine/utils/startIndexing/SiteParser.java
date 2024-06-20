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
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<SiteNode> {

    private static final Object monitor = new Object();
    private static final Logger LOGGER = LogManager.getLogger(SiteParser.class);
    @Getter
    private final URL url;
//    private final String host;
    private final Site site;

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final String userAgent;
    private final String referrer;

    private static final Set<String> URLS_CONTAINER;
    private static final Set<Page> PAGE_BUFFER;

    static {
        URLS_CONTAINER = new CopyOnWriteArraySet<>();
        PAGE_BUFFER = new CopyOnWriteArraySet<>();

    }

    public SiteParser(URL url, Site site, PageRepository pageRepository
            , SiteRepository siteRepository, String userAgent, String referrer) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.url = url;
        this.siteRepository = siteRepository;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    private Set<String> getChildes(URL parent) {
        Set<String> childes = new TreeSet<>();
        Connection connection = Jsoup.connect(parent.toString())
                .maxBodySize(0).timeout(0).ignoreContentType(true);
        Document doc;
        try {
            Thread.sleep((long) (Math.random() * 50 + 100));
            try {
                doc = connection.get();
            } catch (SocketException | HttpStatusException e) {
                attemptToSavePage(parent, 404, "", e);
                return childes;
            }
            attemptToSavePage(parent, connection.response().statusCode(), doc.html(), null);
            Elements elements = doc.select("a[href]");

            for (Element element : elements) {
                String attr = element.attr("abs:href");
                try {
                    if (attr.matches(".*#.*$") || attr.contains("javascript")
                            || attr.contains(" ")
                            || attr.equals(parent.toString())
                            || URLS_CONTAINER.contains(attr)
                            || !parent.getHost().replaceAll("www\\.", "")
                            .equals(new URI(attr).toURL().getHost()
                                    .replaceAll("www\\.", ""))) {
                        continue;
                    }
                    childListAdd(childes, attr);
                }catch (MalformedURLException | URISyntaxException ignored) {}
            }
        } catch (Exception e) {
            LOGGER.error("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
        }
//        LOGGER.info(URLS_CONTAINER.size());
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
                    SiteParser task = new SiteParser(new URI(child).toURL(),
                            site, pageRepository, siteRepository, userAgent, referrer);
                    task.fork();
                    taskList.add(task);
                }
                for (SiteParser task : taskList) {
                    task.join();
                }
            } catch (Exception e) {
                LOGGER.error("{}\n{}\n{}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
            }
            return node;
        }
    }

    private void childListAdd(Set<String> childes, String child) {
        if (!childes.isEmpty()) {
            childes.removeIf(c -> c.contains(child));
        }
        childes.add(child);
    }

    private void attemptToSavePage(URL url, int statusCode, String content, Exception e) {
        synchronized (monitor) {
            if (!URLS_CONTAINER.contains(url.toString())) {
                URLS_CONTAINER.add(url.toString());
                Page page = new Page();
                page.setCode(statusCode);
                page.setSite(site);
                page.setContent(content);
                page.setPath(url.getPath());
                if (!PAGE_BUFFER.contains(page)) {
                    PAGE_BUFFER.add(page);
                    if (PAGE_BUFFER.size() >= 30 && e == null) {
                        pageRepository.saveAll(PAGE_BUFFER);
                        PAGE_BUFFER.clear();
                    }

                } else LOGGER.info("{} {} {}",
                        "Page", page.getPath(), "is already contains in BUFFER");
            } else if (e != null)
                LOGGER.info("{}\n{} {} {}", e.getClass().getSimpleName(),
                        "Url", url.toString(), "is already contains in CONTAINER");
            else LOGGER.info("{} {} {}",
                        "Url", url.toString(), "is already contains in CONTAINER");
        }
    }

    public static synchronized void clearBuffer() {
        PAGE_BUFFER.clear();
        URLS_CONTAINER.clear();
    }

    public static synchronized Set<Page> getBuffer() {
        return PAGE_BUFFER;
    }















//    private Map<Document, Set<String>> getChildes(URL parent) {
//        Set<String> childes = new TreeSet<>();
//        Document doc = null;
//        try {
//        connection = Jsoup.connect(parent.toString()).userAgent(userAgent)
//                .referrer(referrer).maxBodySize(0);
//            Thread.sleep((long) (Math.random() * 50 + 100));
//            doc = connection.ignoreContentType(true).get();
//            Elements elements = doc.select("a[href]");
//            for (Element element : elements) {
//                String attr = element.attr("abs:href");
//                try {
//                    if(!attr.contains(" ")) {
//                        URL urlAttr = new URI(attr).toURL();
//                            if (!attr.matches(".*#.*$") && !attr.contains("javascript")
//                                    && !attr.equals(parent.toString())
//                                    && urlAttr.getHost().equals(root.getHost())
//                                    && !UrlContainer.urlIsContained(urlAttr))
//                            {
//                                childListAdd(childes, attr);
//                            }
////                        }
//                    }
//                }catch (MalformedURLException e) {
//                    continue;
//                }
//            }
//        } catch (SocketException | SocketTimeoutException ex) {
//          LOGGER.info("{} - {}", parent, ex.getMessage());
//        }
//        catch (HttpStatusException httpStatusEx) {
//            LOGGER.info(httpStatusEx.getMessage());
//        } catch (InterruptedException e) {
//            LOGGER.warn("{} \n{} \n{}",e.getClass().getSimpleName(),
//                    e.getMessage(), e.getStackTrace());
//        } catch (Exception e) {
//            LOGGER.error("{} \n{} \n{}",e.getClass().getSimpleName(),
//                    e.getMessage(), e.getStackTrace());
//        }
////        LOGGER.info("{}={}Mb", "used memory", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576);
//        Map<Document, Set<String>> data = new HashMap<>();
//        data.put(doc, childes);
//        return data;
//    }
//
//    @Override
//    protected SiteNode compute() {
//        Map<Document, Set<String>> childes = getChildes(url);
//        if (childes.isEmpty()) {
//            try {
//                return new SiteNode(url, pageRepository, site, siteRepository
//                        , referrer, userAgent, connection, null);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            SiteNode node = null;
//            try {
//                node = new SiteNode(url, pageRepository, site, siteRepository
//                        , referrer, userAgent, connection, childes.keySet().stream().findFirst().get());
//            } catch (ConnectException ex) {
//                LOGGER.info("{} - {}", url.toString(), ex.getMessage());
//            }
//            catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                List<SiteParser> taskList = gettingFork(childes.values().stream().findFirst().get());
//                for (SiteParser task : taskList) {
//                    task.join();
////                    assert node != null;
////                    node.addChild(child);
//                }
//            } catch (CancellationException ignored) {
//            } catch (Exception exception) {
//                LOGGER.error("{} \n{} \n{}",exception.getClass().getSimpleName(),
//                        exception.getMessage(), exception.getStackTrace());
//            }
//            return node;
//        }
//    }
//
//    private List<SiteParser> gettingFork(Set<String> childes) {
//        List<SiteParser> taskList = new ArrayList<>();
//        for (String child : childes) {
//            SiteParser task = null;
//            try {
//                task = new SiteParser
//                                (new URI(child).toURL(), site, pageRepository
//                                        , siteRepository, userAgent, referrer);
//            } catch (MalformedURLException ignored) {} catch (URISyntaxException e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                assert task != null;
//                task.fork();
//                taskList.add(task);
//            } catch (NullPointerException ignored) {
//            }
//        }
//        return taskList;
//    }
//
//    private void childListAdd(Set<String> childes, String child) {
////        try {
////            URL childURL = new URI(child).toURL();
////            if (!childURL.getHost().replaceAll("www\\.", "").equals(host)) {
////                return;
////            }
////        } catch (Exception exception) {
////            LOGGER.error("{} \n {} \n{}",child, exception.getMessage(), exception.getStackTrace());
////        }
//        childes.add(child);
//    }

//    public static void clearContainer(URL url) {
//        UrlContainerClass.clear(url);
//    }
//
//    public static synchronized void addToContainer(URL url) {
//        UrlContainerClass.addToContainer(url);
//            }

//    private static class UrlContainerClass {
//        private static final Map<String, Set<String>> URL_CONTAINER;
//        static {
//            URL_CONTAINER = new ConcurrentHashMap<>();
//
//        }
//
//        private static synchronized void addToContainer(URL url) {
//            if (hostIsContained(url)) URL_CONTAINER.get(url.getHost()).add(url.toString());
//            else {
//                URL_CONTAINER.put(url.getHost(), new HashSet<>());
//                URL_CONTAINER.get(url.getHost()).add(url.toString());
//            }
//        }
//
//        private static synchronized boolean urlIsContained(URL url) {
//            if(!hostIsContained(url)) return false;
//            return URL_CONTAINER.get(url.getHost()).stream()
//                .anyMatch(u -> u.equals(url.toString()));
//        }
//
//        private static synchronized boolean hostIsContained(URL url) {
//            return URL_CONTAINER.keySet().stream()
//                    .anyMatch(h -> h.equals(url.getHost()));
//
//        }
//        private static void clear(URL url){
//            URL_CONTAINER.remove(url.getHost());
//        }

//        private static long getMemoryOfURLContainer() {
//            long totalSum = 0;
//            for(Set<String> set : URL_CONTAINER.values()) {
//                Optional<Integer> sumOpt = set.stream().map(String::length).reduce(Integer::sum);
//                totalSum= totalSum + sumOpt.get();
//            }
//            totalSum = totalSum / 1048578;
//            return totalSum;
//        }
//    }

//    public static synchronized boolean isUrlUnique(URL url) {
//       return !UrlContainerClass.urlIsContained(url);
//    }

//    public static long getMemoryContainer() {
//        return UrlContainerClass.getMemoryOfURLContainer();
//    }
}