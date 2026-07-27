package orbmrkt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вспомогательный класс с константой UUID_REGEX для валидации X-User-Id")
public final class UserId {
    private UserId() {
    }

    @Schema(description = "Регулярное выражение для валидации UUID",
            example = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    public static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
