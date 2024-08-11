package searchengine.services;

import searchengine.dto.Response;

public interface SearchService {
    Response search(String query,
                    String site,
                    Integer offset,
                    Integer limit);
}
