package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class OrderPaymentRequested {
    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private String userId;

    private long amount;

    @JsonProperty("occurred_at")
    private Instant occurredAt;
}
