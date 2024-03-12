package searchengine.services;

import searchengine.utils.Response;

import java.util.concurrent.ExecutionException;

public interface StartIndexingService {

    Response startIndexing() throws InterruptedException;

    Response stopIndexing() throws InterruptedException, ExecutionException;
}
