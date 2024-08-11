package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import searchengine.dto.Response;

import java.util.Set;

@Getter
@ToString
public class SearchResponse extends Response {

    private final int count;

    private final boolean result;

    @Setter
    private Set<SearchData> data;

    public SearchResponse(boolean result, int count, Set<SearchData> data) {
        super(result);
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
