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
    private static final ExecutorService serviceLemmaIndexer;
    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;
    private static final ExecutorService servicePageIndexer;
    private static Map<PageIndexingThread, searchengine.model.entities.Site> threads;
    private static List<Future<String>> futureIndexingList = new ArrayList<>();
    private static List<Future<Integer>> futureLemmanizationList;

    static {
        servicePageIndexer = Executors.newCachedThreadPool();
        serviceLemmaIndexer = Executors.newFixedThreadPool(10);
    }

    @Override
    public StartIndexingResponse startIndexing() throws InterruptedException {
        List<Site> siteList = sites.getSites();
        threads = new TreeMap<>();
        siteList.forEach(s -> {
            searchengine.model.entities.Site site = new searchengine.model.entities.Site();
            site.setName(s.getName());
            site.setUrl(s.getUrl());
            site.setStatus(EnumStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("");
            siteRepository.save(site);
            PageIndexingThread pageIndexingThread = new PageIndexingThread(site.getUrl(), site, pageRepository);
            threads.put(pageIndexingThread, site);
        });
        for (PageIndexingThread pageIndexingThread : threads.keySet())
            futureIndexingList.add(servicePageIndexer.submit(pageIndexingThread));
        while (servicePageIndexer.isTerminated()) {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(3000));
            if(futureIndexingList.stream().filter(f -> f.isCancelled() ||
                    f.isDone()).toList().size() == futureIndexingList.size()) servicePageIndexer.shutdown();
        }
        futureIndexingList = servicePageIndexer.invokeAll(threads.keySet());
        servicePageIndexer.shutdown();
        List<Future<String>> canceledList = futureIndexingList.stream().filter(Future::isCancelled).toList();
        List<searchengine.model.entities.Site> siteArr;
        if (canceledList.size() == siteList.size()) {
            threads.values().forEach(s -> {
                s.setLastError("Indexing is interrupted");
                s.setStatusTime(LocalDateTime.now());
                s.setStatus(EnumStatus.FAILED);
                siteRepository.save(s);
            });
            return new StartIndexingResponse(false, "Indexing has already started or interrupted");
        } else if (!canceledList.isEmpty()) {
            siteArr = threads.values().stream().toList();
            canceledList.forEach(f ->  {
                int index = futureIndexingList.indexOf(f);
                searchengine.model.entities.Site site = siteArr.get(index);
                site.setLastError("Indexing was interrupted");
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(EnumStatus.FAILED);
                siteRepository.save(site);

            });
            return lemmatization();
        } else return lemmatization();
    }

    private StartIndexingResponse lemmatization() throws InterruptedException {
        List<searchengine.model.entities.Site> siteList = siteRepository.findAll();
        List<Thread> threadListFindPages = new ArrayList<>();
        List<Page> pageList = new CopyOnWriteArrayList<>();
        List<LemmaIndexer> lemmaIndexerList = new CopyOnWriteArrayList<>();
        List<searchengine.model.entities.Site> siteFiltredList = siteList.stream().
                filter(s -> s.getStatus().equals(EnumStatus.INDEXING)).toList();
                siteFiltredList.forEach(s -> {
                    Thread thread = new Thread(() -> {
                        pageList.addAll(pageRepository.findPageBySiteId(s.getId()));
                        pageList.forEach(p ->
                                lemmaIndexerList.add(new LemmaIndexer(p, lemmaRepository, indexesRepository)));
                    });
            thread.start();
            threadListFindPages.add(thread);
        });
        threadListFindPages.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        futureLemmanizationList = serviceLemmaIndexer.invokeAll(lemmaIndexerList);
        serviceLemmaIndexer.shutdown();
        futureLemmanizationList.forEach(f -> System.err.println(f.toString() + " - " + f.isDone()));
        if (serviceLemmaIndexer.isTerminated()) {
            siteList.forEach(s -> {
                s.setStatusTime(LocalDateTime.now());
                s.setStatus(EnumStatus.INDEXED);
                siteRepository.save(s);
            });
            return new StartIndexingResponse(true, "");
        }
         else {
            siteList.forEach(s -> {
                s.setLastError("Indexing was interrupted");
                s.setStatusTime(LocalDateTime.now());
                s.setStatus(EnumStatus.FAILED);
                siteRepository.save(s);
            });
            return new StartIndexingResponse(false, "Lemmanization is failed");
        }
    }

    @Override
    public StartIndexingResponse stopIndexing() throws ExecutionException, InterruptedException {
        ExecutorService executorStopService = Executors.newFixedThreadPool(1);
        Future<StartIndexingResponse> startIndexingResponseFuture = executorStopService.submit(() -> {
//            if ( (servicePageIndexer.isShutdown() && !servicePageIndexer.isTerminated()))
//                return new StartIndexingResponse(false, "Indexing has already failed");
//            if (serviceLemmaIndexer.isShutdown() && !serviceLemmaIndexer.isTerminated())
//                return new StartIndexingResponse(false, "Indexing has already failed");
//            if (servicePageIndexer.isTerminated() && serviceLemmaIndexer.isTerminated())
//                return new StartIndexingResponse(false, "Indexing has already finished");
//            else {
            if (futureIndexingList.isEmpty()) {
                return new StartIndexingResponse(false, "Indexing isn't started yet");
            }
                if (!servicePageIndexer.isShutdown()) {
                    // Останавливает работу servicePageIndexer
                    servicePageIndexer.shutdownNow();
                    // Отменяет выполнение всех задач
                   futureIndexingList.forEach(f -> f.cancel(true));
//                    servicePageIndexer.close();
                }
                if (!serviceLemmaIndexer.isShutdown()) {
                    serviceLemmaIndexer.shutdownNow();
                    if (futureLemmanizationList != null)
                        futureLemmanizationList.forEach(f -> f.cancel(true));
//                    serviceLemmaIndexer.close();
                }
                List<searchengine.model.entities.Site> siteList = siteRepository.findAll();
                siteList.forEach(s -> {
                    s.setStatus(EnumStatus.FAILED);
                    s.setStatusTime(LocalDateTime.now());
                    s.setLastError("Indexing is stopped manually");
                    siteRepository.save(s);
                });
//            }
            return new StartIndexingResponse(true, "");
        });
        return startIndexingResponseFuture.get();
    }
}
