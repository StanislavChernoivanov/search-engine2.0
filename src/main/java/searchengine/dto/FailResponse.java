package searchengine.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class FailResponse extends Response {
    public FailResponse(boolean result, String error) {
        super(result);
        this.result = result;
        this.error = error;
    }

    protected boolean result;
    protected String error;
}
