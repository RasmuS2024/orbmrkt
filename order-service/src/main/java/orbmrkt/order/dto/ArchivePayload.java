package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ArchivePayload {

    @JsonProperty("area_of_interest")
    private String areaOfInterest;

    @JsonProperty("capture_date")
    private String captureDate;

    @JsonProperty("sensor_type")
    private String sensorType;
}
