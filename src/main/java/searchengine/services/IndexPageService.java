package searchengine.services;

import searchengine.dto.Response;

import java.io.IOException;

public interface IndexPageService {

    Response indexPage(String url) throws IOException, InterruptedException;
}
