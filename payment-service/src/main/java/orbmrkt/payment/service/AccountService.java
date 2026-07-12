package orbmrkt.payment.service;

import lombok.RequiredArgsConstructor;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.exception.PaymentException;
import orbmrkt.payment.model.AccountEntity;
import orbmrkt.payment.model.ProcessedPaymentEntity;
import orbmrkt.payment.repository.AccountRepository;
import orbmrkt.payment.repository.ProcessedPaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProcessedPaymentRepository processedPaymentRepository;

    @Transactional
    public AccountResponse createAccount(String userId) {
        return accountRepository.findByUserId(userId)
                .map(this::toAccountResponse)
                .orElseGet(() -> {
                    AccountEntity account = new AccountEntity();
                    account.setUserId(userId);
                    account = accountRepository.save(account);
                    return toAccountResponse(account);
                });
    }

    @Transactional
    public BalanceResponse topUp(String userId, long amount) {
        if (amount <= 0) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }

        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));

        account.setBalance(account.getBalance() + amount);
        account = accountRepository.save(account);

        return new BalanceResponse(account.getUserId(), account.getBalance(), "geocredits");
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));

        return new BalanceResponse(account.getUserId(), account.getBalance(), "geocredits");
    }

    public long debit(UUID orderId, String userId, long amount) {
        if (processedPaymentRepository.existsByOrderId(orderId)) {
            AccountEntity account = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new PaymentException(
                            HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
            return account.getBalance();
        }

        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));

        if (account.getBalance() < amount) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "Insufficient balance");
        }

        account.setBalance(account.getBalance() - amount);
        account = accountRepository.save(account);

        ProcessedPaymentEntity processedPayment = new ProcessedPaymentEntity();
        processedPayment.setOrderId(orderId);
        processedPayment.setAccountId(account.getId());
        processedPayment.setAmount(amount);
        processedPaymentRepository.save(processedPayment);

        return account.getBalance();
    }

    private AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(account.getUserId(), account.getBalance(), account.getCreatedAt());
    }
}
