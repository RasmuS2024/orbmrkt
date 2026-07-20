package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
public class OrderPaymentCompleted {
    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private String userId;

    private long amount;

    @JsonProperty("new_balance")
    private long newBalance;
}
