package orbmrkt.payment.service;

import orbmrkt.payment.model.AccountEntity;
import orbmrkt.payment.model.ProcessedPaymentEntity;
import orbmrkt.payment.repository.AccountRepository;
import orbmrkt.payment.repository.ProcessedPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Captor
    private ArgumentCaptor<AccountEntity> accountCaptor;

    @Captor
    private ArgumentCaptor<ProcessedPaymentEntity> paymentCaptor;

    private AccountService accountService;

    private final String userId = "550e8400-e29b-41d4-a716-446655440000";
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, processedPaymentRepository);
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
        verify(processedPaymentRepository).save(paymentCaptor.capture());
        assertEquals(orderId, paymentCaptor.getValue().getOrderId());
        assertEquals(120, paymentCaptor.getValue().getAmount());
    }
}