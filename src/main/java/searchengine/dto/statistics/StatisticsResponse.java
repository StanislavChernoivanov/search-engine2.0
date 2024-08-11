package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.Response;

@Getter
@Setter
public class StatisticsResponse extends Response {
    private boolean result;
    private StatisticsData statistics;

    public StatisticsResponse(boolean result, StatisticsData statistics) {
        super(result);
        this.result = result;
        this.statistics = statistics;
    }
}
