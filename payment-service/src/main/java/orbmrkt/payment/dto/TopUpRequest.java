package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TopUpRequest {
    @JsonProperty("amount")
    private Long amount;
}
