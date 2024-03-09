package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import searchengine.dto.indexPage.IndexPageResponse;
import searchengine.dto.startIndexing.Response;

import java.io.IOException;
import java.net.URL;

public interface IndexPageService {

    Response indexPage(URL url) throws IOException, InterruptedException;
}
