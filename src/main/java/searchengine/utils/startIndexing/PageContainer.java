package searchengine.utils.startIndexing;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.orm.jpa.JpaSystemException;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
public class PageContainer {
    private final Map<String, Page> PAGE_CONTAINER;

    private final PageRepository pageRepository;



    public PageContainer(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
        PAGE_CONTAINER = new HashMap<>();
    }

    public synchronized void clear() {
        PAGE_CONTAINER.clear();
    }

    public synchronized void addPage(URL url, Document document, Site site) {
        if(!isContainsPage(url.getPath())) {
            Page page = new Page();
            page.setPath(url.getPath());
            page.setSite(site);
            if(document != null) {
                page.setContent(document.html());
                page.setCode(document.connection().response().statusCode());
            } else {
                page.setContent("");
                page.setCode(404);
            }
                PAGE_CONTAINER.put(url.getPath(), page);
                log.debug("add page - {}", url.getPath());
                savePagesIfCountEnough();
        }
    }

    private synchronized void savePagesIfCountEnough() {
        if(PAGE_CONTAINER.values().stream().filter(Objects::nonNull).count() >= 30
                || Runtime.getRuntime().totalMemory() / Runtime.getRuntime().freeMemory() > 10)
        {
            savePages();
        }
    }

    public synchronized boolean isContainsPage(String path) {
        return PAGE_CONTAINER.keySet().stream().anyMatch(key -> key.toLowerCase(Locale.ROOT).equals(path.toLowerCase(Locale.ROOT)));
    }

    public synchronized void savePages() {
        Set<Page> pageSet = PAGE_CONTAINER.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        try {
            pageRepository.saveAll(pageSet);
            PAGE_CONTAINER.replaceAll((a, b) -> null);
        } catch(JpaSystemException ex) {
            pageSet.forEach(p -> {
                try {
                    pageRepository.save(p);
                    PAGE_CONTAINER.replace(p.getPath(), null);
                }catch (JpaSystemException e) {
                    log.warn("{}\nPage (path - {}) is removed from buffer", "Packet for query is too large." +
                            " You can change this value on the server" +
                            " by setting the 'max_allowed_packet' variable.", p.getPath());
                    PAGE_CONTAINER.remove(p.getPath());
                }
            });
        }
    }



}
