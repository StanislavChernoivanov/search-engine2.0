package searchengine.utils.startIndexing;
import java.net.URL;


public class SiteNode implements Comparable<SiteNode> {

    private final URL url;


    public SiteNode(URL url) {
        this.url = url;
    }



    @Override
    public int compareTo(SiteNode o) {
        return url.toString().compareTo(o.url.toString());
    }

//    private static final Map<String, Set<String>> URL_CONTAINER = new HashMap<>();
//    private final Set<SiteNode> childNodes = new TreeSet<>();

//    private final URL url;
//    private final PageRepository pageRepository;
//    private final Site site;
//    private static final Set<Page> PAGE_BUFFER;
//    private final SiteRepository siteRepository;
//    private final String userAgent;
//    private final String referrer;
//    private final Connection connection;
//    private Document doc;
//
//    static {
//        PAGE_BUFFER = new CopyOnWriteArraySet<>();
//    }
//
//    public SiteNode(URL url,
//                    PageRepository pageRepository,
//                    Site site,
//                    SiteRepository siteRepository, String referrer
//            , String userAgent, Connection connection, Document doc) throws IOException {
//
//        this.pageRepository = pageRepository;
//        this.site = site;
//        this.url = url;
//        this.siteRepository = siteRepository;
//        this.userAgent = userAgent;
//        this.referrer = referrer;
//        this.connection = connection;
//        this.doc = doc;
////        createAndSavePage();
//    }















//    public void createAndSavePage() {
//        Page page = new Page();
//        synchronized (UrlContainer.class) {
//            if(!UrlContainer.urlIsContained(url)) {
//                UrlContainer.addToContainer(url);
//            try {
//                page.setContent(doc.html());
//                page.setCode(connection.response().statusCode());
//            } catch (IllegalArgumentException e) {
//                page.setContent("");
//                page.setCode(408);
//            } catch (NullPointerException e) {
//                page.setContent("");
//                page.setCode(404);
//            }
//                doc = null;
//                page.setSite(site);
//                page.setPath(url.getPath());
//            save(site, siteRepository, pageRepository, page);
//        }
//        }
//    }
//
//    private static void save(Site site,
//                                          SiteRepository siteRepository,
//                                          PageRepository pageRepository, Page page){
//        try {
//                PAGE_BUFFER.add(page);
//                long free = Runtime.getRuntime().freeMemory() / 1048576;
//                long total = Runtime.getRuntime().totalMemory() / 1048576;
//                double difference = (double) free / total * 100;
//                if (PAGE_BUFFER.size() >= 30 || difference <= 10) {
//                    log.info("\n{}={}%\n{}={}Mb\n{}={}Mb\n",
//                            "free JVM memory left in percent",
//                            String.valueOf(new DecimalFormat("#0.00").format(difference)),
//                            "total JVM memory",
//                            (Runtime.getRuntime().totalMemory() / 1048576),
//                            "free JVM memory",
//                            (Runtime.getRuntime().freeMemory()) / 1048576);
//                    log.info("\n{}={}Mb","buffer memory size", PAGE_BUFFER.stream()
//                            .map(Page::getMemorySize).reduce(Double::sum));
////                    List<String> paths = pageRepository.findPageByManyPaths(PAGE_BUFFER.stream().map(Page::getPath).toList());
//                    site.setStatusTime(LocalDateTime.now());
//                    siteRepository.save(site);
////                    PAGE_BUFFER.stream().filter(p -> !paths.contains(p.getPath())).toList()
//                    pageRepository.saveAll(PAGE_BUFFER);
//                    PAGE_BUFFER.clear();
//                }
////            }
////                }
//        } catch (OutOfMemoryError error) {
//            log.error("{}\n{}\n{}\n{}={}Mb", error.getClass().getSimpleName(),
//                    error.getMessage(), error.getStackTrace(),"buffer memory size", PAGE_BUFFER.stream()
//                            .map(Page::getMemorySize).reduce(Double::sum));
//        }
//    }
//
//    public static synchronized Set<Page> getPagesBuffer() {
//        return PAGE_BUFFER;
//    }
//
//    public static synchronized void clearPagesBuffer() {
//        PAGE_BUFFER.clear();
//    }
//
////    private static synchronized void addToContainer(URL url) {
////        if (hostIsContained(url)) URL_CONTAINER.get(url.getHost()).add(url.toString());
////        else {
////            URL_CONTAINER.put(url.getHost(), new HashSet<>());
////            URL_CONTAINER.get(url.getHost()).add(url.toString());
////        }
////    }
//
////    public void addChild(SiteNode child) {
////        synchronized (childNodes) {
////            childNodes.add(child);
////        }
////    }
////
////    public static synchronized boolean urlIsContained(URL url) {
////        if(!hostIsContained(url)) return false;
////        return URL_CONTAINER.get(url.getHost()).stream()
////                .anyMatch(u -> u.equals(url.toString()));
////    }
////
////    private static synchronized boolean hostIsContained(URL url) {
////        return URL_CONTAINER.keySet().stream()
////                .anyMatch(h -> h.equals(url.getHost()));
////
////    }
////    public static void clear(URL url){
////        URL_CONTAINER.remove(url.getHost());
////    }
//
//
//    @Override
//    public int compareTo(SiteNode o) {
//        return url.toString().compareTo(o.url.toString());
//    }
}

//                    log.info("\n{}={}%\n{}={}Mb\n{}={}Mb\n",
//                            "free JVM memory left in percent",
//                                String.valueOf(new DecimalFormat("#0.00").format(difference)),
//                            "total JVM memory",
//                            (Runtime.getRuntime().totalMemory() / 1048576),
//                            "free JVM memory",
//                            (Runtime.getRuntime().freeMemory()) / 1048576);
//                    log.info("\n{}={}Mb","buffer memory size", pageBuffer.stream()
//                                    .map(Page::getMemorySize).reduce(Double::sum));
