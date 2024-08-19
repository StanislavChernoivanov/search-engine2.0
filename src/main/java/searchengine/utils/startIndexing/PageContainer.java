package searchengine.utils.startIndexing;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.PageRepository;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope("prototype")
@NoArgsConstructor
public class PageContainer {
    private final Map<String, Page> pageContainer = new HashMap<>();
    @Setter
    @Autowired
    private PageRepository pageRepository;

    public synchronized void clear() {
        pageContainer.clear();
    }

    public synchronized void addPage(URL url, Document document, Site site) {
        if (!isContainsPage(url.getPath())) {
            Page page = new Page();
            page.setPath(url.getPath());
            page.setSite(site);
            if (document != null) {
                page.setContent(document.html());
                page.setCode(document.connection().response().statusCode());
            } else {
                page.setContent("");
                page.setCode(404);
            }
            pageContainer.put(url.getPath(), page);
            log.debug("add page - {}", url.getPath());
            savePagesIfMemoryIsFull();
        }
    }

    private synchronized void savePagesIfMemoryIsFull() {
        if (pageContainer.values().stream().filter(Objects::nonNull).count() >= 30
                || Runtime.getRuntime().totalMemory() / Runtime.getRuntime().freeMemory() > 10) {
            savePages();
        }
    }

    public synchronized boolean isContainsPage(String path) {
        return pageContainer.keySet().stream().anyMatch(key -> key.toLowerCase(Locale.ROOT).equals(path.toLowerCase(Locale.ROOT)));
    }

    public synchronized void savePages() {
        Set<Page> pageSet = pageContainer.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        try {
            pageRepository.saveAll(pageSet);
            pageContainer.replaceAll((a, b) -> null);
        } catch (JpaSystemException ex) {
            pageSet.forEach(p -> {
                try {
                    pageRepository.save(p);
                    pageContainer.replace(p.getPath(), null);
                } catch (JpaSystemException e) {
                    log.warn("{}\nPage (path - {}) is removed from buffer", "Packet for query is too large." +
                            " You can change this value on the server" +
                            " by setting the 'max_allowed_packet' variable.", p.getPath());
                    pageContainer.remove(p.getPath());
                }
            });
        }
    }


}
