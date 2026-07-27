package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Стандартный ответ API (для ошибок)")
public class ApiResponse<T> {

    @Schema(description = "Данные ответа (только при успехе)")
    private T data;

    @JsonProperty("error_code")
    @Schema(description = "Код ошибки", example = "INVALID_PRICE")
    private String errorCode;

    @Schema(description = "Описание ошибки", example = "Price must be greater than 0")
    private String message;

    @Schema(description = "Время ответа", example = "2026-07-27T12:00:00Z")
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(null, errorCode, message, Instant.now());
    }
}