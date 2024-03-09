package searchengine.services;
import searchengine.search.SearchResponse;

public interface SearchService {
    SearchResponse search(String query,
                          String site,
                          Integer offset,
                          Integer limit);
}
