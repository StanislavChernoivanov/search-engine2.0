package searchengine.utils.search;
import lombok.*;
import searchengine.utils.Response;

import java.util.Set;

@Getter
@ToString
public class SearchResponse extends Response {

    private final int count;

    @Setter
    private Set<SearchResultData> searchResultData;

    public SearchResponse(boolean result, String error, int count, Set<SearchResultData> searchResultData) {
        super(result, error);
        this.count = count;
        this.searchResultData = searchResultData;
    }
}
