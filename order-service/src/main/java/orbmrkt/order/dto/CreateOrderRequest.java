package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import orbmrkt.dto.ProductType;

import java.util.Map;

@Data
@Schema(description = "Запрос на создание нового заказа")
public class CreateOrderRequest {

    @JsonProperty("product_type")
    @Schema(description = "Тип продукта", example = "TASKING", requiredMode = Schema.RequiredMode.REQUIRED)
    private ProductType productType;

    @Schema(description = "Цена заказа в geocredits", example = "1500",
            minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long price;

    @Schema(
        description = "Параметры задачи. Зависит от productType:\n" +
                      "- ARCHIVE → ArchivePayload (aoi, capture_date, sensor_type)\n" +
                      "- TASKING → TaskingPayload (aoi, time_window, sensor_type)\n" +
                      "- MONITORING → MonitoringPayload (aoi, cadence, duration_days)",
        example = "{\"aoi\": \"POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))\", " +
                  "\"time_window\": {\"from\": \"2026-08-01\", \"to\": \"2026-08-15\"}, " +
                  "\"sensor_type\": \"OPTICAL\"}"
    )
    private Map<String, Object> payload;
}
