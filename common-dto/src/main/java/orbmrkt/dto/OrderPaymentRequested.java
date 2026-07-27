package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Schema(description = "Событие запроса платежа (order-service → payment-service, Kafka)")
public class OrderPaymentRequested {
    @JsonProperty("event_id")
    @Schema(description = "Идентификатор события (для дедупликации)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID eventId;

    @JsonProperty("order_id")
    @Schema(description = "Идентификатор заказа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId;

    @JsonProperty("user_id")
    @Schema(description = "Идентификатор пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String userId;

    @Schema(description = "Сумма к списанию в geocredits", example = "1500")
    private long amount;

    @JsonProperty("occurred_at")
    @Schema(description = "Время события", example = "2026-07-27T12:00:00Z")
    private Instant occurredAt;
}
