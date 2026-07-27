package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о балансе пользователя")
public class BalanceResponse {
    @JsonProperty("user_id")
    @Schema(description = "Идентификатор пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String userId;

    @JsonProperty("balance")
    @Schema(description = "Текущий баланс в geocredits", example = "10000")
    private long balance;

    @JsonProperty("currency")
    @Schema(description = "Валюта", example = "geocredits")
    private String currency;

    public BalanceResponse(String userId, long balance) {
        this.userId = userId;
        this.balance = balance;
        this.currency = "geocredits";
    }
}
