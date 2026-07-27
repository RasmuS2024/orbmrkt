package orbmrkt.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import orbmrkt.dto.UserId;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.dto.TopUpRequest;
import orbmrkt.payment.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Tag(name = "Accounts", description = "Управление аккаунтом и балансом пользователя")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    @Operation(summary = "Создать аккаунт", description = "Создаёт платёжный аккаунт для пользователя. " +
            "Идемпотентно – повторный вызов возвращает 200 OK с существующим аккаунтом.")
    @ApiResponse(responseCode = "200", description = "Аккаунт создан (или уже существует)",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    @ApiResponse(responseCode = "400", description = "MISSING_USER_ID - отсутствует или невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "409", description = "ACCOUNT_ALREADY_EXISTS - счёт для user_id уже есть "
            + "(конкурентное создание)",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<AccountResponse> createAccount(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId) {
        AccountResponse response = accountService.createAccount(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/top-up")
    @Operation(summary = "Пополнить баланс",
            description = "Пополняет баланс пользователя на указанную сумму (amount > 0)")
    @ApiResponse(responseCode = "200", description = "Баланс пополнен",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @ApiResponse(responseCode = "400", description = "INVALID_AMOUNT или MISSING_USER_ID - невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "ACCOUNT_NOT_FOUND - счёт не создан",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<BalanceResponse> topUp(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId,
            @RequestBody TopUpRequest request) {
        BalanceResponse response = accountService.topUp(userId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/balance")
    @Operation(summary = "Получить баланс", description = "Возвращает текущий баланс пользователя")
    @ApiResponse(responseCode = "200", description = "Текущий баланс",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @ApiResponse(responseCode = "400", description = "MISSING_USER_ID - отсутствует или невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "ACCOUNT_NOT_FOUND - счёт не найден",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<BalanceResponse> getBalance(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId) {
        BalanceResponse response = accountService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
}
