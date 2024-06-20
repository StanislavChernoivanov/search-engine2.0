package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.entities.Page;
import searchengine.dto.Response;
import searchengine.utils.startIndexing.*;
import searchengine.dto.FailResponse;
import searchengine.model.entities.EnumStatus;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

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
    @Value("${connection-properties.userAgent}")
    private String userAgent;
    @Value("${connection-properties.referrer}")
    private String referrer;


    @Override
    public Response startIndexing() throws InterruptedException {
        if (!indexingThreadMap.isEmpty()) return new FailResponse(false, "Индексация уже запущена");
        siteRepository.deleteAll();
        List<Site> siteList = sites.getSites();
        if(saverOrRefresher != null) saverOrRefresher.clearBuffers();
        Thread indexing = new Indexing(siteList);
        indexing.start();
        return new Response(true);
    }

    private void startConcurrencyIndexing(List<Site> siteList) throws InterruptedException {
        int indexedPagesAmount = indexingThreadMap.size();
        Site siteConfig = siteList.get(indexedPagesAmount);
        searchengine.model.entities.Site site = new searchengine.model.entities.Site();
        site.setName(siteConfig.getName());
        site.setUrl(siteConfig.getUrl());
        site.setStatus(EnumStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("");
        siteRepository.save(site);
        SiteIndexer pagesIndexingThread
                = new SiteIndexer(site.getUrl(), site, pageRepository, siteRepository, userAgent, referrer);
        pagesIndexingThread.start();
        pagesIndexingThread.join();
        indexingThreadMap.put(pagesIndexingThread, site);
        if(indexedPagesAmount < siteList.size() - 1) startConcurrencyIndexing(siteList);
    }

    @Override
    public Response stopIndexing() {
        if(indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive)) {
            indexingThreadMap.keySet().forEach(t -> t.getPool().shutdownNow());
            indexingThreadMap.keySet().forEach(Thread::interrupt);
            return new Response(true);
        }
        else {
            saverOrRefresher.isInterrupted = true;
            return new Response(true);
        }
    }


    @RequiredArgsConstructor
    class Indexing extends Thread {
        private  final List<Site> siteList;

        @Override
        public void run() {
            log.info("{} {}", "Indexing started at",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
            long time = System.currentTimeMillis();
            try {
                startConcurrencyIndexing(siteList);
            } catch (InterruptedException e) {
                makeFailedStatus(indexingThreadMap);
                return;
            } finally {
                pageRepository.saveAll(SiteParser.getBuffer());
                SiteParser.clearBuffer();
                indexingThreadMap.clear();
            }
            log.info("{} {}(for {})", "Pages are loaded at",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")),
                    (System.currentTimeMillis() - time) / 1000);
            try {
                lemmatization();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        private void makeFailedStatus(Map<SiteIndexer, searchengine.model.entities.Site> indexerSiteMap) {
            if(saverOrRefresher != null) saverOrRefresher.isInterrupted = false;
            indexerSiteMap.values().forEach(site -> {
                site.setLastError("Индексация прервана");
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(EnumStatus.FAILED);
            });
            siteRepository.saveAll(indexerSiteMap.values());
            log.info("{} {}","Indexing is interrupted at ",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
        }
        private void lemmatization() throws InterruptedException {
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
            if(saverOrRefresher.isInterrupted) {
                makeFailedStatus(indexingThreadMap);
            } else {
                log.info("{} {}(for {})", "Indexing is over at ",
                        LocalDateTime.now().format(DateTimeFormatter
                                .ofPattern("HH:mm:ss dd.MM.yyyy")),
                        (System.currentTimeMillis() - timeStamp) / 1000);
                indexingThreadMap.values().forEach(site -> {
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.INDEXED);
                    siteRepository.save(site);
                });
            }
            indexingThreadMap.clear();
            threads.clear();
        }
    }
}
