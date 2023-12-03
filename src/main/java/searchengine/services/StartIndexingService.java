package searchengine.services;

import searchengine.dto.startIndexing.StartIndexingResponse;

import java.util.concurrent.ExecutionException;

public interface StartIndexingService {

    StartIndexingResponse startIndexing() throws InterruptedException;

    StartIndexingResponse stopIndexing() throws InterruptedException, ExecutionException;
}
