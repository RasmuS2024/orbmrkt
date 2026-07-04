package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("balance")
    private long balance;

    @JsonProperty("currency")
    private String currency;

    public BalanceResponse(String userId, long balance) {
        this.userId = userId;
        this.balance = balance;
        this.currency = "geocredits";
    }
}
