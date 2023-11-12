package searchengine.dto.startIndexing;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class StartIndexingResponse {
    public StartIndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    private boolean result;
    private String error;
}
