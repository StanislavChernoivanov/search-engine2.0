package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.startIndexing.CreateSession;
import searchengine.dto.startIndexing.PageIndexingThread;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.model.EnumStatus;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StartIndexingServiceImpl implements StartIndexingService {

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private StartIndexingResponse startIndexingResponse;

    private Logger logger = LogManager.getRootLogger();
    private final SitesList sites;
    private static List<Site> siteList = new ArrayList<>();
    private static Map<Thread, searchengine.model.Site> threadMap = new HashMap<>();


    @Override
    public void deleteData()
    {
        siteRepository.deleteAll();
    }

    @Override
    public StartIndexingResponse startIndexing()
    {
        startIndexingResponse = new StartIndexingResponse();
        do {
            if (siteList.isEmpty()) {
                siteList = sites.getSites();
                for (int i = 0; i < siteList.size(); i++) {
                    searchengine.model.Site site = new searchengine.model.Site();
                    site.setName(siteList.get(i).getName());
                    site.setUrl(siteList.get(i).getUrl());
                    site.setStatus(EnumStatus.INDEXING);
                    site.setStatusTime(LocalDateTime.now());
                    site.setLastError("");
                    siteRepository.save(site);
                    PageIndexingThread pageIndexingThread = new PageIndexingThread(site.getUrl(), site);
                    pageIndexingThread.start();
                    threadMap.put(pageIndexingThread, site);
                }
            }
                indexingSiteCompletedOrInterrupted();
            startIndexingResponse.setResult(false);
            startIndexingResponse.setError("Индексация уже запущена");
        } while (!isIndexed());
        startIndexingResponse.setError("");
        startIndexingResponse.setResult(true);
        return startIndexingResponse;
    }

    private void indexingSiteCompletedOrInterrupted() {
        Set<Thread> keySet = threadMap.keySet();
        for(Thread thread : keySet) {
            if(!thread.isAlive() && !thread.isInterrupted() &&
                    threadMap.get(thread).getStatus().equals(EnumStatus.INDEXING)) {
                threadMap.get(thread).setStatusTime(LocalDateTime.now());
                threadMap.get(thread).setStatus(EnumStatus.INDEXED);
                siteRepository.save(threadMap.get(thread));
//                logger.info("Сайт " + threadMap.get(thread).getName() + " проиндексирован");
            } else if (thread.isInterrupted()) {
                threadMap.get(thread).setStatus(EnumStatus.FAILED);
                threadMap.get(thread).setStatusTime(LocalDateTime.now());
                threadMap.get(thread).setLastError("Индексация прервана");
                siteRepository.save(threadMap.get(thread));
                logger.info("Индексация сайта " + threadMap.get(thread).getName() + " прервана");
            } else {
                threadMap.get(thread).setStatusTime(LocalDateTime.now());
                threadMap.get(thread).setStatus(EnumStatus.INDEXING);
                siteRepository.save(threadMap.get(thread));
            }
        }
    }


    private boolean isIndexed() {
        int i = 0;
        if(!siteList.isEmpty()) {
            Iterable<searchengine.model.Site> iterable = siteRepository.findAll();
            for (searchengine.model.Site site : iterable) {
                boolean isIndexed = site.getStatus().equals(EnumStatus.INDEXED) ? true : false;
                if(isIndexed) i++;
            }
            if(i == siteList.size()) return true;
        }
        return false;
    }

    @Override
    public StartIndexingResponse stopIndexing() {
        int countAliveThreads = 0;
        Set<Thread> keySet = threadMap.keySet();
        if (!keySet.isEmpty()) {
            for(Thread t : keySet) {
                if(t.isAlive()) {
                    t.interrupt();
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
