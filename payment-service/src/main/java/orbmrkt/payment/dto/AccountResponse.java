package orbmrkt.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Schema(description = "Информация о платёжном аккаунте")
public class AccountResponse {
    @JsonProperty("user_id")
    @Schema(description = "Идентификатор пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String userId;

    @JsonProperty("balance")
    @Schema(description = "Текущий баланс в geocredits", example = "10000")
    private long balance;

    @JsonProperty("created_at")
    @Schema(description = "Дата и время создания аккаунта", example = "2026-07-27T12:00:00Z")
    private Instant createdAt;
}
