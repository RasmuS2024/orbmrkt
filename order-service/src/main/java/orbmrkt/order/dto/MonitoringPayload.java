package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MonitoringPayload {

    private String aoi;

    private String cadence;

    @JsonProperty("duration_days")
    private int durationDays;
}
