package searchengine.controllers;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StartIndexingService;
import searchengine.services.StatisticsService;

import java.util.Optional;

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

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam Optional<String> url) {
        if (!url.isPresent()) return new ResponseEntity(HttpStatus.NOT_FOUND);
        return new ResponseEntity (HttpStatus.OK);
    }
}
