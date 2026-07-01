package orbmrkt.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderPaymentRequestedEvent {
    private UUID orderId;
    private BigDecimal amount;
    private String userId;
}
