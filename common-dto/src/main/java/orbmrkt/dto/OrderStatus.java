package orbmrkt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус заказа", enumAsRef = true)
public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    REJECTED
}
