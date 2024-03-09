package searchengine.dto.startIndexing;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class Response {
    public Response(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    private boolean result;
    private String error;
}
