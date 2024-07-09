package searchengine.dto.search;

import lombok.Getter;

@lombok.Data
@Getter
public class SearchData implements Comparable<SearchData> {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private StringBuilder snippet;
    private Float relevance;

    @Override
    public int compareTo(SearchData o) {
        return o.relevance.compareTo(relevance);
    }
}
