package searchengine.utils.search;
import lombok.*;
import searchengine.utils.Response;

import java.util.Set;

@Getter
public class SearchResponse extends Response {

    private final int count;
    private final Set<SearchResultData> searchResultData;

    public SearchResponse(boolean result, String error, int count, Set<SearchResultData> searchResultData) {
        super(result, error);
        this.count = count;
        this.searchResultData = searchResultData;
    }


    @Override
    public String toString() {
        if(result)
            return "SearchResponse{" +
                "result=" + true +
                ", error='" + error + '\'' +
                ", count=" + count +
                ", searchResultData=" + searchResultData +
                '}';
        else return "SearchResponse{" +
                "result=" + false +
                ", error='" + error + '\'' +
                '}';
    }
}
