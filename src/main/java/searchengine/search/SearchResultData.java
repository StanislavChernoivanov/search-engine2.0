package searchengine.search;

import lombok.Data;
import lombok.Getter;

import javax.swing.text.html.HTML;

@Data
@Getter
public class SearchResultData implements Comparable<SearchResultData> {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private StringBuilder snippet;
    private Float relevance;

    @Override
    public int compareTo(SearchResultData o) {
        return o.relevance.compareTo(relevance);
    }
}
