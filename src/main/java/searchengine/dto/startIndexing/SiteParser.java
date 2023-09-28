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
import searchengine.dto.indexPage.LemmaFinder;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
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
            Thread.sleep((long) (Math.random() * 500) + 300);
            Elements elements = doc.select("a[href]");
            Session session = CreateSession.getSession();
            for (Element element : elements) {
                String attr = element.attr("abs:href");
                if (attr.contains("http") || attr.contains("https")) {
                    URL attrUrl = new URL(attr);
                    if (!attrUrl.getHost().replaceAll("www\\.", "").equals(host) ||
                            attr.matches(".*#$") || attr.contains("javascript") ||
                            !attr.contains(url.toString()) || attr.equals(url.toString()) ||
                            attrUrl.getPath().equals("/") ||
                            !CreateSession.urlIsUnique(new URL(attr).getPath(), session)) {
                        continue;
                    }
                    LemmaFinder finder = new LemmaFinder();
                    // Получаем мап с леммами
                    Map<String, Integer> lemmas = finder.collectLemmas(doc.text());
                    Transaction t = session.beginTransaction();
                    try{
                        Page page = new Page();
                        page.setCode(connection.response().statusCode());
                        page.setContent(doc.html());
                        page.setSite(site);
                        page.setPath(attrUrl.getPath());
                        session.save(page);
                        t.commit();
                        // Из мапы получаем сет и в цикле проводим операции по созданию и обновлению каждой леммы
                        Set<String> keySet = lemmas.keySet();
                        for (String key : keySet) {
                            // Если лемма не существует создаем лемму и индекс и сохраняем в бд
                            if(!CreateSession.lemmaIsExist(key, session)) {
                                t.begin();
                                Lemma lemma = new Lemma();
                                lemma.setSite(site);
                                lemma.setLemma(key);
                                lemma.setFrequency(1);
                                session.save(lemma);
                                t.commit();
                                t.begin();
                                Indexes index = new Indexes();
                                index.setPage(page);
                                index.setLemma(lemma);
                                index.setRank(lemmas.get(key));
                                session.save(index);
                                t.commit();
                                // Если не существует только индекс то у леммы увеличиваем frequency на 1 и создаем индекс
                            } else if (!CreateSession.indexIsExist(CreateSession.
                                    findLemma(key, session).getId(), page.getId())) {
                                t.begin();
                                Lemma lemma = CreateSession.findLemma(key, session);
                                lemma.setFrequency(lemma.getFrequency() + 1);
                                session.update(lemma);
                                t.commit();
                                t.begin();
                                Indexes index = new Indexes();
                                index.setPage(page);
                                index.setLemma(lemma);
                                index.setRank(lemmas.get(key));
                                session.save(index);
                                t.commit();
                            } else {
                                // Если существует и лемма и индекс то просто увеличиваем частоту на 1
                                t.begin();
                                Lemma lemma = CreateSession.findLemma(key,session);
                                lemma.setFrequency(lemma.getFrequency() + 1);
                                session.update(lemma);
                                t.commit();
                            }
                        }
                    } catch (Exception e) {
                        t.rollback();
                        e.printStackTrace();
                    }
                    childListAdd(childes, attr);
                    path = attr;
                }
            }
            session.close();
        } catch (Exception e) {
            LOGGER.error("{} \n {} \n{}",path, e.getMessage(), e.getStackTrace());
        }
        return childes;
    }

//    private void addLemmaAndIndexIntoDB(String pathPage, Session session,
//                                        Map<String, Integer> lemmas) throws InterruptedException {
//        Set<String> keySet = lemmas.keySet();
//        Page page = CreateSession.getPageOnPath(pathPage, session);
//        for (String key : keySet) {
//            Lemma lemma;
//            Indexes index = new Indexes();
//            if(!CreateSession.lemmaIsExist(key)) {
//                Transaction t = session.beginTransaction();
//                lemma = new Lemma();
//                lemma.setSite(site);
//                lemma.setLemma(key);
//                lemma.setFrequency(1);
//                session.save(lemma);
//                t.commit();
//                t = session.beginTransaction();
//                index.setPage(page);
//                index.setLemma(lemma);
//                index.setRank(lemmas.get(key));
//                session.save(index);
//                t.commit();
//            } else if (!CreateSession.indexIsExist(CreateSession.
//                    findLemma(key).getId(), page.getId())) {
//                Transaction t = session.beginTransaction();
//                lemma = CreateSession.findLemma(key);
//                lemma.setFrequency(lemma.getFrequency() + 1);
//                session.update(lemma);
//                t.commit();
//                t = session.beginTransaction();
//                index.setPage(page);
//                index.setLemma(lemma);
//                index.setRank(lemmas.get(key));
//                session.save(index);
//                t.commit();
//            } else {
//                Transaction t = session.beginTransaction();
//                lemma = CreateSession.findLemma(key);
//                lemma.setFrequency(lemma.getFrequency() + 1);
//                session.update(lemma);
//                t.commit();
//            }
//        }
//    }

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
        if (isChild.isEmpty()) {
            try {
                URL childURL = new URL(child);
                if (!childURL.getHost().replaceAll("www\\.", "").equals(host)) {
                    return;
                }
            } catch (Exception exception) {
                LOGGER.error("{} \n {} \n{}",child, exception.getMessage(), exception.getStackTrace());
            }
            if (!childes.isEmpty()) {
                childes.removeIf(c -> c.contains(child));
            }
            childes.add(child);
        }
    }
}