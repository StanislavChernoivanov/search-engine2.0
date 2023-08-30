package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.startIndexing.PageIndexingThread;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.model.entities.EnumStatus;
import searchengine.model.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class StartIndexingServiceImpl implements StartIndexingService {
    private final SiteRepository siteRepository;

    private StartIndexingResponse startIndexingResponse;

    private final SitesList sites;
    private static List<Site> siteList = new ArrayList<>();
    private static final Map<Future<String>, searchengine.model.entities.Site> FUTURES_MAP = new ConcurrentHashMap<>();
    private static Set<Future<String>> keySet = new HashSet<>();
    private static ExecutorService service;

    private static int indexedAmount = 0;

    @Override
    public void deleteData()
    {
        siteRepository.deleteAll();
    }

    @Override
    public StartIndexingResponse startIndexing() {
        startIndexingResponse = new StartIndexingResponse();
        try {
            do {
                if (siteList.isEmpty()) {
                    siteList = sites.getSites();
                    service = Executors.newFixedThreadPool(siteList.size());
                    siteList.forEach(s -> {
                        searchengine.model.entities.Site site = new searchengine.model.entities.Site();
                        site.setName(s.getName());
                        site.setUrl(s.getUrl());
                        site.setStatus(EnumStatus.INDEXING);
                        site.setStatusTime(LocalDateTime.now());
                        site.setLastError("");
                        siteRepository.save(site);
                        PageIndexingThread pageIndexingThread = new PageIndexingThread(site.getUrl(), site);
                        Future<String> future = service.submit(pageIndexingThread);
                        FUTURES_MAP.put(future, site);
                    });
                }
            } while (!indexingSiteCompletedOrInterrupted());
            if(indexingSiteCompletedOrInterrupted()) {
                startIndexingResponse.setError("");
                startIndexingResponse.setResult(true);
            } else {
                startIndexingResponse.setResult(false);
                startIndexingResponse.setError("Индексация уже запущена");
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } return startIndexingResponse;
    }

    private boolean indexingSiteCompletedOrInterrupted() throws InterruptedException {
        keySet = FUTURES_MAP.keySet();
        for (Future<String> f : keySet) {
            Optional<searchengine.model.entities.Site> optional = siteRepository.findById(FUTURES_MAP.get(f).getId());
            if(optional.isPresent()) {
                searchengine.model.entities.Site site = optional.get();
                if (!f.isCancelled() && !f.isDone()) {
                    Thread.sleep(3000);
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.INDEXING);
                    siteRepository.save(site);
                } else if (f.isCancelled()) {
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.FAILED);
                    site.setLastError("Индексация прервана");
                    siteRepository.save(site);
                } else if (f.isDone() && FUTURES_MAP.get(f).getStatus().equals(EnumStatus.INDEXING)) {
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.INDEXED);
                    siteRepository.save(site);
                    indexedAmount++;
                    try {
                        System.out.println(f.get());
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return indexedAmount == siteList.size();
    }

    @Override
    public StartIndexingResponse stopIndexing() {
        int countAliveThreads = 0;
        if (!keySet.isEmpty()) {
            for(Future<String> f : keySet) {
                Optional<searchengine.model.entities.Site> optional = siteRepository.findById(FUTURES_MAP.get(f).getId());
                if(optional.isPresent() && !f.isCancelled() && !f.isDone()) {
                    searchengine.model.entities.Site site = optional.get();
                    site.setStatusTime(LocalDateTime.now());
                    site.setStatus(EnumStatus.FAILED);
                    site.setLastError("Индексация прервана пользователем");
                    siteRepository.save(site);
                    f.cancel(true);
                    countAliveThreads++;
                }
            }
            if(countAliveThreads > 0) {
                startIndexingResponse.setResult(true);
                startIndexingResponse.setError("");
                return startIndexingResponse;
            }
            startIndexingResponse.setResult(false);
            startIndexingResponse.setError("Индексация не запущена");
        } return startIndexingResponse;
    }
}
