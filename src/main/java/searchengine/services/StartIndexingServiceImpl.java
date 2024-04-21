package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.utils.startIndexing.LemmaIndexer;
import searchengine.utils.startIndexing.PagesIndexingThread;
import searchengine.utils.Response;
import searchengine.model.entities.EnumStatus;
import searchengine.model.entities.Page;
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
@Slf4j
public class StartIndexingServiceImpl implements StartIndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;
    private Map<Thread, searchengine.model.entities.Site> indexingThreadMap;
    private List<Thread> lemmanizationThreadList;
    private ExecutorService service;
    private List<Runnable> neverCommencedList;


    @Override
    public Response startIndexing() throws InterruptedException {
        clearTables();
        List<Site> siteList = sites.getSites();
        indexingThreadMap = new HashMap<>();
        long time = System.currentTimeMillis();
        siteList.forEach(s -> {
            searchengine.model.entities.Site site = new searchengine.model.entities.Site();
            site.setName(s.getName());
            site.setUrl(s.getUrl());
            site.setStatus(EnumStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("");
            siteRepository.save(site);
            Thread thread = new Thread(new PagesIndexingThread(site.getUrl(), site, pageRepository, siteRepository));
            thread.start();
            indexingThreadMap.put(thread, site);
        });
        do {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(3000));
        } while (indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive));
        if(!SiteNode.getRemainingPages().isEmpty())
            pageRepository.saveAllAndFlush(SiteNode.getRemainingPages());
        log.info("Pages are loaded for " + ((System.currentTimeMillis() - time) / 1000));
        if (indexingThreadMap.keySet().stream().allMatch(Thread::isInterrupted))
            return new Response(false, "Индексация прервана");
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

        service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Set<Page> pageSet = Collections.synchronizedSet(new HashSet<>());
        Collection<searchengine.model.entities.Site> doneCorrectlySet = doneCorrectlyMap.values();
        doneCorrectlySet.forEach(s -> pageSet.addAll(pageRepository.findPageBySiteId(s.getId())));
        lemmanizationThreadList = new ArrayList<>();
        double timeStamp = System.currentTimeMillis();
        SaverOrRefresher saverOrRefresher = new SaverOrRefresher(lemmaRepository, indexesRepository);
        pageSet.forEach(p -> {
            Thread task = new Thread(new LemmaIndexer(p, saverOrRefresher));
            service.submit(new LemmaIndexer(p, saverOrRefresher));
            lemmanizationThreadList.add(task);
        });
        while(lemmanizationThreadList.stream().anyMatch(Thread::isAlive)) Thread.sleep(5000);
        service.shutdown();
        saverOrRefresher.saveRemainedLemmasInDB();
        while(!service.isTerminated()) {
            if(neverCommencedList != null) {
                doneCorrectlySet.forEach(site -> {
                    site.setLastError("Индексация прервана");
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.FAILED);
                    siteRepository.save(site);
                });
                neverCommencedList.clear();
                neverCommencedList = null;
                return new Response(false, "Индексация прервана");
            }
            Thread.sleep(3000);
        }
        System.out.println(System.currentTimeMillis() - timeStamp);
        doneCorrectlySet.forEach(site -> {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(EnumStatus.INDEXED);
            siteRepository.save(site);
        });
        return new Response(true, "");
    }


    private void clearTables() {
        indexesRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Override
    public Response stopIndexing() {
        if(indexingThreadMap == null) return new Response(false, "Индексация не запускалась");
        else if(indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive)) {
            indexingThreadMap.keySet().forEach(Thread::interrupt);
            return new Response(true, "");
        }
        else if (service != null && !service.isTerminated()) {
            neverCommencedList = service.shutdownNow();
            indexingThreadMap.clear();
            neverCommencedList.clear();
            return new Response(true, "");
        } else
            indexingThreadMap.clear();
            return new Response(false, "Индексация не запускалась или провалилась");
    }
}
