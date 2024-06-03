package searchengine.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.entities.Page;
import searchengine.dto.Response;
import searchengine.utils.startIndexing.LemmaIndexer;
import searchengine.utils.startIndexing.SiteIndexer;
import searchengine.dto.FailResponse;
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
    @Value("${connection-properties.userAgent}")
    private String userAgent;
    @Value("${connection-properties.referrer}")
    private String referrer;


    @Override
    public Response startIndexing() throws InterruptedException {
        if (!indexingThreadMap.isEmpty()) return new FailResponse(false, "Индексация уже запущена");
        log.info("{} {}", "Indexing started at",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")));
        siteRepository.deleteAll();
        List<Site> siteList = sites.getSites();
        if(saverOrRefresher != null) saverOrRefresher.clearBuffers();
        siteList.forEach(this::startConcurrencyIndexing);
        Thread indexing = new Indexing();
        indexing.start();
        return new Response(true);
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
                = new SiteIndexer(site.getUrl(), site, pageRepository, siteRepository, userAgent, referrer);
        pagesIndexingThread.start();
        indexingThreadMap.put(pagesIndexingThread, site);
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



    class Indexing extends Thread {
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            do {
                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(5000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while (indexingThreadMap.keySet().stream().anyMatch(Thread::isAlive));
            if (!SiteNode.getPagesBuffer().isEmpty()) {
                pageRepository.saveAll(SiteNode.getPagesBuffer());
                SiteNode.clearPagesBuffer();
            }
            if (indexingThreadMap.keySet().stream().allMatch(Thread::isInterrupted)) {
                getFailedResponse(indexingThreadMap);
                indexingThreadMap.clear();
            } else {
                log.info("{} {}(for {})", "Pages are loaded at",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")),
                        (System.currentTimeMillis() - time) / 1000);
                try {
                    lemmatization();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        private void getFailedResponse (Map<SiteIndexer, searchengine.model.entities.Site> indexerSiteMap) {
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
                getFailedResponse(indexingThreadMap);
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
