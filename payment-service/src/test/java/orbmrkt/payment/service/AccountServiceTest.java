package orbmrkt.payment.service;

import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.exception.PaymentException;
import orbmrkt.payment.model.AccountEntity;
import orbmrkt.payment.repository.AccountRepository;
import orbmrkt.payment.repository.ProcessedPaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedPaymentRepository processedPaymentRepository;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<AccountEntity> accountCaptor;

    private final String userId = "test-user";
    private final UUID orderId = UUID.randomUUID();

    @Test
    void createAccount_newUser_success() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> {
            AccountEntity saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AccountResponse response = accountService.createAccount(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(accountRepository).save(any());
    }

    @Test
    void createAccount_existingUser_returnsExisting() {
        AccountEntity existing = new AccountEntity();
        existing.setId(UUID.randomUUID());
        existing.setUserId(userId);
        existing.setBalance(500);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        AccountResponse response = accountService.createAccount(userId);

        assertEquals(userId, response.getUserId());
        assertEquals(500, response.getBalance());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void topUp_validAmount_increasesBalance() {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.setBalance(0);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BalanceResponse response = accountService.topUp(userId, 1000);

        assertEquals(userId, response.getUserId());
        assertEquals(1000, response.getBalance());
        assertEquals("geocredits", response.getCurrency());
        verify(accountRepository).save(account);
    }

    @Test
    void topUp_nonPositiveAmount_throws() {
        assertThrows(PaymentException.class, () -> accountService.topUp(userId, 0));
        assertThrows(PaymentException.class, () -> accountService.topUp(userId, -10));
        verify(accountRepository, never()).findByUserId(any());
    }

    @Test
    void topUp_accountNotFound_throws() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> accountService.topUp(userId, 100));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void getBalance_accountFound_returns() {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.setBalance(500);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(userId);

        assertEquals(userId, response.getUserId());
        assertEquals(500, response.getBalance());
    }

    @Test
    void getBalance_accountNotFound_throws() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> accountService.getBalance(userId));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void debit_sufficientFunds_decreasesBalance() {
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setBalance(1000);
        when(processedPaymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long newBalance = accountService.debit(orderId, userId, 120);

        assertEquals(880, newBalance);
        verify(processedPaymentRepository).save(any());
    }

    @Test
    void debit_insufficientFunds_throws() {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.setBalance(50);
        when(processedPaymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> accountService.debit(orderId, userId, 120));
        assertEquals("INSUFFICIENT_BALANCE", ex.getErrorCode());
    }

    @Test
    void debit_duplicateOrderId_skips() {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.setBalance(1000);
        when(processedPaymentRepository.existsByOrderId(orderId)).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        long newBalance = accountService.debit(orderId, userId, 120);

        assertEquals(1000, newBalance);
        verify(accountRepository, never()).save(any());
        verify(processedPaymentRepository, never()).save(any());
    }

    @Test
    void debit_optimisticLock_propagates() {
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setBalance(1000);
        when(processedPaymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenThrow(OptimisticLockingFailureException.class);

        assertThrows(OptimisticLockingFailureException.class,
                () -> accountService.debit(orderId, userId, 120));
    }
}
