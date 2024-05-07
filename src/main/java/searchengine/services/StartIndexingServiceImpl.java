package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
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
    private Map<SiteIndexer, searchengine.model.entities.Site> indexingThreadMap;
    private ExecutorService executorService;
    private List<Runnable> neverCommencedTasks;

    private SaverOrRefresher saverOrRefresher;


    @Override
    public Response startIndexing() throws InterruptedException {
        siteRepository.deleteAll();
        List<Site> siteList = sites.getSites();
        indexingThreadMap = new HashMap<>();
        long time = System.currentTimeMillis();
        siteList.forEach(this::startConcurrencyIndexing);
        do {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(5000));
            indexingThreadMap.keySet().forEach(t -> log.info("{} - {}", t.getName(), t.isAlive()));
        } while (indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive));
        if(!SiteNode.getPagesBuffer().isEmpty()) {
            pageRepository.saveAll(SiteNode.getPagesBuffer());
            SiteNode.clearPagesBuffer();
        }
        log.info("Pages are loaded for " + ((System.currentTimeMillis() - time) / 1000));
        if (indexingThreadMap.keySet().stream().allMatch(Thread::isInterrupted)) {
            setFailedStatus(indexingThreadMap.values());
            return new Response(false, "Индексация прервана");
        }
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
        return lemmatization(doneCorrectlyMap);
    }

    private Response lemmatization(Map<Thread,
            searchengine.model.entities.Site> doneCorrectlyMap) throws InterruptedException {
        executorService = Executors.newCachedThreadPool();
        List<searchengine.model.entities.Site> actualSitesList = siteRepository.findAll();
        double timeStamp = System.currentTimeMillis();
        saverOrRefresher = SaverOrRefresher.getInstance(lemmaRepository, indexesRepository);
        actualSitesList.stream().filter(s -> s.getStatus().equals(EnumStatus.INDEXING))
                .forEach(s ->  {
                    Thread thread = new Thread(new LemmaIndexer
                            (s, saverOrRefresher, pageRepository));
                    executorService.submit(thread);
                    thread.start();
                });
        executorService.shutdown();
        while(!executorService.isTerminated()) {
            if(neverCommencedTasks != null && neverCommencedTasks.size() ==
                    actualSitesList.stream().filter(s -> s.getStatus().equals(EnumStatus.INDEXING)).count()) {
                neverCommencedTasks = null;
                setFailedStatus(doneCorrectlyMap.values());
                return new Response(false, "Индексация прервана");
            }
            Thread.sleep(3000);
        }
        saverOrRefresher.saveRemainedLemmasInDB();
        System.out.println(System.currentTimeMillis() - timeStamp);
        doneCorrectlyMap.values().forEach(site -> {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(EnumStatus.INDEXED);
            siteRepository.save(site);
        });
        return new Response(true, "");
    }

    @Override
    public Response stopIndexing() {
        if(indexingThreadMap == null) return new Response(false, "Индексация не запускалась");
        else if(indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive)) {
            indexingThreadMap.keySet().forEach(t -> t.getPool().shutdownNow());
            indexingThreadMap.keySet().forEach(Thread::interrupt);
            return new Response(true, "");
        }
        else if (executorService != null && !executorService.isTerminated()) {
            neverCommencedTasks = executorService.shutdownNow();
//            indexingThreadMap.clear();
//            saverOrRefresher.saveRemainedLemmasInDB();
            return new Response(true, "");
        } else
            indexingThreadMap.clear();
        return new Response(false, "Индексация не запускалась");
    }

    private void setFailedStatus(Collection<searchengine.model.entities.Site> failedSitesSet) {
        failedSitesSet.forEach(site -> {
            site.setLastError("Индексация прервана");
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(EnumStatus.FAILED);
            siteRepository.save(site);
        });
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


    private void clearDB() {
        indexesRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        siteRepository.deleteAll();
    }
}
