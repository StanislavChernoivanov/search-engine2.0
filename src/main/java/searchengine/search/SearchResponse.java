package searchengine.search;
import lombok.Data;
import java.util.Set;

@Data
public class SearchResponse {

    private boolean result;
    private String error;
    private int count;
    private Set<SearchResultData> searchResultData;

}
