package orbmrkt.payment.controller;

import lombok.RequiredArgsConstructor;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.dto.TopUpRequest;
import orbmrkt.payment.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestHeader("X-User-Id") String userId) {
        AccountResponse response = accountService.createAccount(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/top-up")
    public ResponseEntity<BalanceResponse> topUp(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TopUpRequest request) {
        BalanceResponse response = accountService.topUp(userId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @RequestHeader("X-User-Id") String userId) {
        BalanceResponse response = accountService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
}
