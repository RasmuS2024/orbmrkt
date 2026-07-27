package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Параметры целевой съёмки (productType = TASKING)")
public class TaskingPayload {

    @Schema(description = "Area of Interest (WKT-полигон)", example = "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))")
    private String aoi;

    @JsonProperty("time_window")
    @Schema(description = "Временное окно для съёмки")
    private TimeWindow timeWindow;

    @JsonProperty("sensor_type")
    @Schema(description = "Тип сенсора", example = "OPTICAL")
    private String sensorType;
}
