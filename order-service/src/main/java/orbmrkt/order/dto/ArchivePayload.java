package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ArchivePayload {

    private String aoi;

    @JsonProperty("capture_date")
    private String captureDate;

    @JsonProperty("sensor_type")
    private String sensorType;
}
