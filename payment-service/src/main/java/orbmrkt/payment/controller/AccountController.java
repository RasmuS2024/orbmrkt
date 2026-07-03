package orbmrkt.payment.controller;

import lombok.RequiredArgsConstructor;
import orbmrkt.dto.ApiResponse;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.dto.TopUpRequest;
import orbmrkt.payment.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @RequestHeader("X-User-Id") String userId) {
        boolean exists = accountService.accountExists(userId);
        AccountResponse response = accountService.createAccount(userId);
        if (exists) {
            return ResponseEntity.ok(ApiResponse.success(response));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/accounts/top-up")
    public ResponseEntity<ApiResponse<BalanceResponse>> topUp(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TopUpRequest request) {
        BalanceResponse response = accountService.topUp(userId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/accounts/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @RequestHeader("X-User-Id") String userId) {
        BalanceResponse response = accountService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
