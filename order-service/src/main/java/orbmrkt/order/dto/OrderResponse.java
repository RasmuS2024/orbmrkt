package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OrderResponse {

    @JsonProperty("order_id")
    private UUID orderId;

    private String status;

    @JsonProperty("product_type")
    private String productType;

    private long price;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("failure_reason")
    private String failureReason;
}
