package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Параметры архивной съёмки (productType = ARCHIVE)")
public class ArchivePayload {

    @Schema(description = "Area of Interest (WKT-полигон)", example = "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))")
    private String aoi;

    @JsonProperty("capture_date")
    @Schema(description = "Дата съёмки (ISO 8601)", example = "2026-06-15")
    private String captureDate;

    @JsonProperty("sensor_type")
    @Schema(description = "Тип сенсора", example = "OPTICAL")
    private String sensorType;
}
