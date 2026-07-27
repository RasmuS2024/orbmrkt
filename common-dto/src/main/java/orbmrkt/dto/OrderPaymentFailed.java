package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

@Data
@Schema(description = "Событие неуспешного платежа (payment-service → order-service, Kafka)")
public class OrderPaymentFailed {
    @JsonProperty("event_id")
    @Schema(description = "Идентификатор события (для дедупликации)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID eventId;

    @JsonProperty("order_id")
    @Schema(description = "Идентификатор заказа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId;

    @JsonProperty("user_id")
    @Schema(description = "Идентификатор пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String userId;

    @Schema(description = "Причина отказа", example = "INSUFFICIENT_BALANCE")
    private String reason;
}
