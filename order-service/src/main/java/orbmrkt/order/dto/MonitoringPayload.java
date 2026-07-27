package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Параметры мониторинга (productType = MONITORING)")
public class MonitoringPayload {

    @Schema(description = "Area of Interest (WKT-полигон)", example = "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))")
    private String aoi;

    @Schema(description = "Периодичность съёмки", example = "DAILY")
    private String cadence;

    @JsonProperty("duration_days")
    @Schema(description = "Длительность мониторинга в днях", example = "30")
    private int durationDays;
}
