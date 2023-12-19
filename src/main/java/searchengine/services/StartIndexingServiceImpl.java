package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexPage.LemmaIndexer;
import searchengine.dto.startIndexing.PageIndexingThread;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.model.entities.EnumStatus;
import searchengine.model.entities.Page;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
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
    private static Map<Thread, searchengine.model.entities.Site> indexingThreadMap;
    private static List<Thread> lemmanizationThreadList;


    @Override
    public StartIndexingResponse startIndexing() throws InterruptedException {
        List<Site> siteList = sites.getSites();
        indexingThreadMap = new HashMap<>();
        siteList.forEach(s -> {
            searchengine.model.entities.Site site = new searchengine.model.entities.Site();
            site.setName(s.getName());
            site.setUrl(s.getUrl());
            site.setStatus(EnumStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("");
            siteRepository.save(site);
            Thread thread = new Thread(new PageIndexingThread(site.getUrl(), site, pageRepository));
            thread.start();
            indexingThreadMap.put(thread, site);
        });
        do {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(3000));
        } while (indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive));
        if (indexingThreadMap.keySet().stream().allMatch(Thread::isInterrupted))
            return new StartIndexingResponse(false, "Indexing was interrupted");
        indexingThreadMap.keySet().stream().filter(Thread::isInterrupted).forEach(e -> {
            searchengine.model.entities.Site site = indexingThreadMap.get(e);
            site.setStatus(EnumStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Indexing was interrupted");
            siteRepository.save(site);
        });
        Map<Thread, searchengine.model.entities.Site> doneCorrectlyMap = new HashMap<>();
        indexingThreadMap.keySet().stream().filter(thread -> !thread.isInterrupted()).forEach(t -> doneCorrectlyMap.put(t, indexingThreadMap.get(t)));
        return lemmatization(doneCorrectlyMap);
    }

    private StartIndexingResponse lemmatization(Map<Thread,
            searchengine.model.entities.Site> doneCorrectlyMap) throws InterruptedException {

        ExecutorService service = Executors.newCachedThreadPool();
        List<Thread> threadListFindPages = new ArrayList<>();
        Set<Page> pageSet = Collections.synchronizedSet(new HashSet<>());
        Collection<searchengine.model.entities.Site> doneCorrectlySet = doneCorrectlyMap.values();
        doneCorrectlySet.forEach(s -> {
            Thread thread = new Thread(() -> {
                pageSet.addAll(pageRepository.findPageBySiteId(s.getId()));
            });
            thread.start();
            threadListFindPages.add(thread);
        });
        threadListFindPages.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                t.interrupt();
            }
        });
        lemmanizationThreadList = new ArrayList<>();
        pageSet.forEach(p -> {
            Thread thread = new Thread(new LemmaIndexer(p, lemmaRepository, indexesRepository));
            service.submit(thread);
            lemmanizationThreadList.add(thread);
        });
        do {
            Thread.sleep(3000);
        } while (!service.isTerminated());
        service.shutdown();
        if (lemmanizationThreadList.stream().filter(Thread::isInterrupted).count() >= (lemmanizationThreadList.size() / 4)) {
            doneCorrectlySet.forEach(site -> {
                site.setLastError("Indexing was interrupted");
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(EnumStatus.FAILED);
                siteRepository.save(site);
            });
            return new StartIndexingResponse(false, "Indexing was interrupted");
        } else {
            doneCorrectlySet.forEach(site -> {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(EnumStatus.INDEXED);
                siteRepository.save(site);
            });
            return new StartIndexingResponse(true, "");
        }
    }

    @Override
    public StartIndexingResponse stopIndexing() {
//            if ( (servicePageIndexer.isShutdown() && !servicePageIndexer.isTerminated()))
//                return new StartIndexingResponse(false, "Indexing has already failed");
//            if (serviceLemmaIndexer.isShutdown() && !serviceLemmaIndexer.isTerminated())
//                return new StartIndexingResponse(false, "Indexing has already failed");
//            if (servicePageIndexer.isTerminated() && serviceLemmaIndexer.isTerminated())
//                return new StartIndexingResponse(false, "Indexing has already finished");
//            else {
//        if (futureIndexingList.isEmpty()) {
//            return new StartIndexingResponse(false, "Indexing isn't started yet");
//        }
//        // TODO: 07.12.2023 think about fixing the method
//        if (!servicePageIndexer.isShutdown()) {
//            futureIndexingList.forEach(f -> f.cancel(true));
//            servicePageIndexer.shutdownNow();
//            servicePageIndexer.close();
//        }
//        if (!serviceLemmaIndexer.isShutdown()) {
//            if (futureLemmanizationList != null) futureLemmanizationList.forEach(f -> f.cancel(true));
//            serviceLemmaIndexer.shutdownNow();
//            serviceLemmaIndexer.close();
//        }
//            List<searchengine.model.entities.Site> siteList = siteRepository.findAll();
//            siteList.forEach(s -> {
//                s.setStatus(EnumStatus.FAILED);
//                s.setStatusTime(LocalDateTime.now());
//                s.setLastError("Indexing is stopped manually");
//                siteRepository.save(s);
//            });
        return new StartIndexingResponse(true, "");
    }
}
