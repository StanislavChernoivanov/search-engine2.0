package searchengine.services;

import searchengine.dto.startIndexing.Response;

import java.util.concurrent.ExecutionException;

public interface StartIndexingService {

    Response startIndexing() throws InterruptedException;

    Response stopIndexing() throws InterruptedException, ExecutionException;
}
