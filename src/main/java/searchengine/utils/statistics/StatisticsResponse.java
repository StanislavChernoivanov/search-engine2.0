package searchengine.utils.statistics;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.utils.Response;

@Getter
@Setter
public class StatisticsResponse extends Response {
    private boolean result;
    private StatisticsData statistics;

    public StatisticsResponse(boolean result, String error, StatisticsData statistics) {
        super(result, error);
        this.statistics = statistics;
    }
}
