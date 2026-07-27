package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Временное окно для съёмки")
public class TimeWindow {

    @Schema(description = "Начало временного окна (ISO 8601)", example = "2026-08-01")
    private String from;

    @Schema(description = "Конец временного окна (ISO 8601)", example = "2026-08-15")
    private String to;
}
