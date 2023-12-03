package searchengine.controllers;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.StartIndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;

    private final StartIndexingService startIndexingService;

    private final IndexPageService indexPageService;

    public ApiController(StatisticsService statisticsService, StartIndexingService startIndexingService, IndexPageService indexPageService) {
        this.statisticsService = statisticsService;
        this.startIndexingService = startIndexingService;
        this.indexPageService = indexPageService;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing() throws InterruptedException {
        return ResponseEntity.ok(startIndexingService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<StartIndexingResponse> stopIndexing() throws InterruptedException, ExecutionException {
        return ResponseEntity.ok(startIndexingService.stopIndexing());
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String strUrl,
                                       LuceneMorphology luceneMorphology) throws IOException {
        return ResponseEntity.ok(indexPageService.indexPage(new URL(strUrl),
                new RussianLuceneMorphology()));
    }
}
