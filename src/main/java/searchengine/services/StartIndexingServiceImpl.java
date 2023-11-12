package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexPage.LemmaIndexer;
import searchengine.dto.startIndexing.PageIndexingThread;
import searchengine.dto.startIndexing.HibernateUtil;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.model.entities.EnumStatus;
import searchengine.model.entities.Page;
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
    private ExecutorService serviceLemmaIndexer;
    private ExecutorService servicePageIndexer;
    private static Map<PageIndexingThread, searchengine.model.entities.Site> threads;

    @Override
    public StartIndexingResponse startIndexing() throws InterruptedException {
        List<Site> siteList = sites.getSites();
        servicePageIndexer = Executors.newCachedThreadPool();
        threads = new HashMap<>();
        siteList.forEach(s -> {
            searchengine.model.entities.Site site = new searchengine.model.entities.Site();
            site.setName(s.getName());
            site.setUrl(s.getUrl());
            site.setStatus(EnumStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("");
            siteRepository.save(site);
            PageIndexingThread pageIndexingThread = new PageIndexingThread(site.getUrl(), site);
            threads.put(pageIndexingThread, site);
        });
        List<Future<String>> futureList = servicePageIndexer.invokeAll(threads.keySet());
        servicePageIndexer.shutdown();
//        if (servicePageIndexer.isTerminated()) {
//            threads.values().forEach(s -> {
//            s.setLastError("Indexing was interrupted");
//            s.setStatusTime(LocalDateTime.now());
//            s.setStatus(EnumStatus.FAILED);
//            siteRepository.save(s);
//            });
//            return new StartIndexingResponse(false, "Indexing has already started or interrupted");
//        }
        serviceLemmaIndexer = Executors.newFixedThreadPool(5);
        List<LemmaIndexer> lemmaIndexerList = new ArrayList<>();
        for (Future<String> f : futureList) {
            Optional<searchengine.model.entities.Site> siteOptional =
                    threads.values().stream().
                            filter(s -> s.getId() == futureList.indexOf(f) + 1).
                            findFirst();
            if (siteOptional.isPresent()) {
                searchengine.model.entities.Site site = siteOptional.get();
                if (f.isCancelled()) {
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.FAILED);
                    site.setLastError("Индексация прервана");
                    siteRepository.save(site);
                } else if (f.isDone()) {
                    site.setStatusTime(LocalDateTime.now());
                    List<Page> pageList = pageRepository.findAll().stream()
                            .filter(p -> p.getSite().getId() == site.getId()).toList();
                    pageList.forEach(p -> lemmaIndexerList.add(new LemmaIndexer(p)));
                } else System.out.println("Site not found");
            }
        }
        serviceLemmaIndexer.invokeAll(lemmaIndexerList);
        serviceLemmaIndexer.shutdown();
        HibernateUtil.getRegistry().close();
//        if (serviceLemmaIndexer) {
//            log.error("Indexing interrupted");
//            threads.values().forEach(s -> {
//                s.setLastError("Indexing was interrupted");
//                s.setStatusTime(LocalDateTime.now());
//                s.setStatus(EnumStatus.FAILED);
//                siteRepository.save(s);
//            });
//            return new StartIndexingResponse(false, "Indexing interrupted");
//        } else
        return new StartIndexingResponse(true, "");
    }

    @Override
    public StartIndexingResponse stopIndexing() {
        if(servicePageIndexer.isShutdown()
                || serviceLemmaIndexer.isShutdown()
                || (servicePageIndexer.isTerminated() && serviceLemmaIndexer.isTerminated()))
            return new StartIndexingResponse(false, "Indexing is not running");
        List<searchengine.model.entities.Site> siteList = siteRepository.findAll();
        siteList.forEach(s -> {
            s.setStatus(EnumStatus.FAILED);
            s.setStatusTime(LocalDateTime.now());
            s.setLastError("indexing is interrupted");
            siteRepository.save(s);
        });
        return new StartIndexingResponse(true, "");
    }
}
