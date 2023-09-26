package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import searchengine.dto.indexPage.IndexPageResponse;

import java.io.IOException;
import java.net.URL;

public interface IndexPageService {

    IndexPageResponse indexPage(URL url, LuceneMorphology luceneMorphology) throws IOException;
}
