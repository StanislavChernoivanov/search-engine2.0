package searchengine.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StartIndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final StartIndexingService startIndexingService;

    public ApiController(StatisticsService statisticsService, StartIndexingService startIndexingService) {
        this.statisticsService = statisticsService;
        this.startIndexingService = startIndexingService;
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing() throws InterruptedException {
        return ResponseEntity.ok(startIndexingService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<StartIndexingResponse> stopIndexing() {
        return ResponseEntity.ok(startIndexingService.stopIndexing());
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
