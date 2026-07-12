package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskingPayload {

    private String aoi;

    @JsonProperty("time_window")
    private TimeWindow timeWindow;

    @JsonProperty("sensor_type")
    private String sensorType;
}
