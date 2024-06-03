package searchengine.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.IndexPageService;
import searchengine.services.SearchService;
import searchengine.services.StartIndexingService;
import searchengine.services.StatisticsService;
import searchengine.dto.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final StartIndexingService startIndexingService;

    public ApiController(StatisticsService statisticsService,
                         StartIndexingService startIndexingService,
                         IndexPageService indexPageService,
                         SearchService searchService) {
        this.statisticsService = statisticsService;
        this.startIndexingService = startIndexingService;
        this.indexPageService = indexPageService;
        this.searchService = searchService;
    }

    private final IndexPageService indexPageService;
    private final SearchService searchService;



    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() throws InterruptedException {
        return ResponseEntity.ok(startIndexingService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() throws InterruptedException, ExecutionException {
        return ResponseEntity.ok(startIndexingService.stopIndexing());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Response> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam(name = "url") String strUrl)
            throws IOException, InterruptedException {
        return ResponseEntity.ok(indexPageService.indexPage(strUrl));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam(name = "query", required = false) String query,
                               @RequestParam(name = "site", required = false) String site,
                               @RequestParam(name = "offset", required = false) Integer offset,
                               @RequestParam(name = "limit", required = false) Integer limit)  {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }

}
