package searchengine.services;

import searchengine.dto.startIndexing.StartIndexingResponse;

public interface StartIndexingService {

    StartIndexingResponse startIndexing() throws InterruptedException;

    StartIndexingResponse stopIndexing();
}
