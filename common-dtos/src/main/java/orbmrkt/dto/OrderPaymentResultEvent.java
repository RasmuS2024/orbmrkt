package orbmrkt.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderPaymentResultEvent {
    private UUID orderId;
    private PaymentStatus status;
    private String failureReason;
}
