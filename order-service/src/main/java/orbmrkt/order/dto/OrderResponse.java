package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Schema(description = "Информация о заказе")
public class OrderResponse {

    @JsonProperty("order_id")
    @Schema(description = "Уникальный идентификатор заказа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId;

    @Schema(description = "Статус заказа", example = "CREATED",
            allowableValues = {"CREATED", "PAYMENT_PENDING", "PAID", "PAYMENT_FAILED", "REJECTED"})
    private String status;

    @JsonProperty("product_type")
    @Schema(description = "Тип продукта", example = "TASKING")
    private String productType;

    @Schema(description = "Цена заказа в geocredits", example = "1500")
    private long price;

    @JsonProperty("created_at")
    @Schema(description = "Дата и время создания заказа", example = "2026-07-27T12:00:00Z")
    private Instant createdAt;

    @JsonProperty("failure_reason")
    @Schema(description = "Причина отклонения заказа (заполнена при статусе REJECTED)", example = "INVALID_PRICE")
    private String failureReason;
}
