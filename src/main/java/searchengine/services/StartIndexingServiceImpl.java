package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.StaleStateException;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.entities.Page;
import searchengine.utils.startIndexing.LemmaIndexer;
import searchengine.utils.startIndexing.SiteIndexer;
import searchengine.utils.Response;
import searchengine.model.entities.EnumStatus;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.utils.startIndexing.SaverOrRefresher;
import searchengine.utils.startIndexing.SiteNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class StartIndexingServiceImpl implements StartIndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;
    private final Map<SiteIndexer, searchengine.model.entities.Site> indexingThreadMap = new HashMap<>();
    private final List<Thread> threads = new ArrayList<>();
    private SaverOrRefresher saverOrRefresher;


    @Override
    public Response startIndexing() throws InterruptedException {
        indexingThreadMap.clear();
        log.info("{} {}","Indexing started at",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
            siteRepository.deleteAll();
        List<Site> siteList = sites.getSites();
        long time = System.currentTimeMillis();
        siteList.forEach(this::startConcurrencyIndexing);
        do {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(5000));
        } while (indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive));
        if(!SiteNode.getPagesBuffer().isEmpty()) {
            pageRepository.saveAll(SiteNode.getPagesBuffer());
            SiteNode.clearPagesBuffer();
        }
        if (indexingThreadMap.keySet().stream().allMatch(Thread::isInterrupted)) {
            setFailedStatus(indexingThreadMap.values());
            log.info("{} {}","Indexing is interrupted at",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
            return new Response(false, "Индексация прервана");
        }
        log.info("{} {}(for {})","Pages are loaded at",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")),
            (System.currentTimeMillis() - time) / 1000);
        indexingThreadMap.keySet().stream().filter(Thread::isInterrupted).forEach(e -> {
            searchengine.model.entities.Site site = indexingThreadMap.get(e);
            site.setStatus(EnumStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Индексация прервана");
            siteRepository.save(site);
        });
        Map<Thread, searchengine.model.entities.Site> doneCorrectlyMap = new HashMap<>();
        indexingThreadMap.keySet().stream().filter(thread ->
                !thread.isInterrupted()).forEach(t -> doneCorrectlyMap.put(t, indexingThreadMap.get(t)));
        indexingThreadMap.clear();
        return lemmatization(doneCorrectlyMap);
    }

    private Response lemmatization(Map<Thread,
            searchengine.model.entities.Site> doneCorrectlyMap) throws InterruptedException {
        List<searchengine.model.entities.Site> actualSitesList = siteRepository.findAll();
        double timeStamp = System.currentTimeMillis();
        saverOrRefresher = SaverOrRefresher.getInstance(lemmaRepository, indexesRepository);

        actualSitesList.stream().filter(s -> s.getStatus().equals(EnumStatus.INDEXING))
                .forEach(s ->  {
                    List<Page> pageList = pageRepository.findPageBySiteId(s.getId());
                    Thread thread = new Thread(new LemmaIndexer
                            (s, saverOrRefresher, pageList));
                    thread.start();
                    threads.add(thread);
                });
        while(threads.stream().anyMatch(Thread::isAlive))
            Thread.sleep(3000);
        if(saverOrRefresher.isInterrupted) return getFailedResponse(doneCorrectlyMap);
        log.info("{} {}(for {})","Indexing is over at ",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")),
                (System.currentTimeMillis() - timeStamp) / 1000);
        doneCorrectlyMap.values().forEach(site -> {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(EnumStatus.INDEXED);
            siteRepository.save(site);
        });
        return new Response(true, "");
    }

    @Override
    public Response stopIndexing() {
        if(!indexingThreadMap.isEmpty() && indexingThreadMap.keySet().stream().anyMatch(t -> !t.isAlive()))
            return new Response(false, "Индексация не запускалась");
        else if(indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive)) {
            indexingThreadMap.keySet().forEach(t -> t.getPool().shutdownNow());
            indexingThreadMap.keySet().forEach(Thread::interrupt);
            return new Response(true, "");
        }
        else if (!threads.isEmpty()) {
            saverOrRefresher.isInterrupted = true;
            threads.clear();
            return new Response(true, "");
        }
        return new Response(false, "Индексация не запускалась");
    }

    private Response getFailedResponse (Map<Thread, searchengine.model.entities.Site> doneCorrectlyMap) {
        saverOrRefresher.isInterrupted = false;
        setFailedStatus(doneCorrectlyMap.values());
        log.info("{} {}","Indexing is interrupted at ",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
        return new Response(false, "Индексация прервана");
    }

    private void setFailedStatus(Collection<searchengine.model.entities.Site> failedSitesSet) {
        failedSitesSet.forEach(site -> {
            site.setLastError("Индексация прервана");
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(EnumStatus.FAILED);
        });
        siteRepository.saveAll(failedSitesSet);
    }

    private void startConcurrencyIndexing(Site configSite) {
        searchengine.model.entities.Site site = new searchengine.model.entities.Site();
        site.setName(configSite.getName());
        site.setUrl(configSite.getUrl());
        site.setStatus(EnumStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("");
        siteRepository.save(site);
        SiteIndexer pagesIndexingThread
                = new SiteIndexer(site.getUrl(), site, pageRepository, siteRepository);
        pagesIndexingThread.start();
        indexingThreadMap.put(pagesIndexingThread, site);
    }
}
