package searchengine.services;

import searchengine.dto.startIndexing.StartIndexingResponse;

public interface StartIndexingService {

    void deleteData();

    StartIndexingResponse startIndexing() throws InterruptedException;

    StartIndexingResponse stopIndexing();
}
