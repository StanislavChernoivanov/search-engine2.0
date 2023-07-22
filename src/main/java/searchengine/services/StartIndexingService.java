package searchengine.services;

import searchengine.dto.startIndexing.StartIndexingResponse;

public interface StartIndexingService {

    void deleteData();

    StartIndexingResponse startIndexing();

    StartIndexingResponse stopIndexing();
}
