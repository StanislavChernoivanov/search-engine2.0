package searchengine.services;
import searchengine.utils.Response;

public interface SearchService {
    Response search(String query,
                    String site,
                    Integer offset,
                    Integer limit);
}
