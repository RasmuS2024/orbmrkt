package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AccountResponse {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("balance")
    private long balance;

    @JsonProperty("created_at")
    private Instant createdAt;
}
