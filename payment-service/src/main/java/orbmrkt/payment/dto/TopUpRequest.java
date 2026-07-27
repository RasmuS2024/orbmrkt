package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на пополнение баланса")
public class TopUpRequest {
    @JsonProperty("amount")
    @Schema(description = "Сумма пополнения в geocredits", example = "5000",
            minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long amount;
}
