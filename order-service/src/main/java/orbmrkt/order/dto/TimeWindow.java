package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimeWindow {

    private String from;

    private String to;
}
