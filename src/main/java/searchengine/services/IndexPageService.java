package searchengine.services;

import searchengine.utils.Response;

import java.io.IOException;
import java.net.URL;

public interface IndexPageService {

    Response indexPage(URL url) throws IOException, InterruptedException;
}
