package orbmrkt.payment.messaging;

import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.payment.exception.PaymentException;
import orbmrkt.payment.model.InboxEntity;
import orbmrkt.payment.repository.InboxRepository;
import orbmrkt.payment.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private InboxRepository inboxRepository;

    @InjectMocks
    private PaymentEventConsumer consumer;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final String userId = "test-user";

    private OrderPaymentRequested event() {
        OrderPaymentRequested event = new OrderPaymentRequested();
        event.setEventId(eventId);
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setAmount(BigDecimal.valueOf(120));
        event.setOccurredAt(Instant.now());
        return event;
    }

    @Test
    void handlePaymentRequested_success_publishesCompleted() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(accountService.debit(orderId, userId, 120)).thenReturn(880L);

        consumer.handlePaymentRequested(event());

        verify(paymentEventPublisher).publishPaymentCompleted(orderId, userId, BigDecimal.valueOf(120), 880);
        verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), any());
    }

    @Test
    void handlePaymentRequested_insufficientFunds_publishesFailed() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(accountService.debit(orderId, userId, 120))
                .thenThrow(new PaymentException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "Insufficient balance"));

        consumer.handlePaymentRequested(event());

        verify(paymentEventPublisher).publishPaymentFailed(orderId, userId, "INSUFFICIENT_BALANCE");
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), any(), anyLong());
    }

    @Test
    void handlePaymentRequested_duplicateEventId_skips() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(true);

        consumer.handlePaymentRequested(event());

        verify(accountService, never()).debit(any(), any(), anyLong());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), any(), anyLong());
        verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), any());
    }

    @Test
    void handlePaymentRequested_concurrentDuplicate_skips() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        doThrow(DataIntegrityViolationException.class).when(inboxRepository).save(any(InboxEntity.class));

        consumer.handlePaymentRequested(event());

        verify(accountService, never()).debit(any(), any(), anyLong());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), any(), anyLong());
        verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), any());
    }

    @Test
    void handlePaymentRequested_unexpectedException_publishesInternalError() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(accountService.debit(orderId, userId, 120)).thenThrow(new RuntimeException("DB failure"));

        consumer.handlePaymentRequested(event());

        verify(paymentEventPublisher).publishPaymentFailed(orderId, userId, "INTERNAL_ERROR");
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), any(), anyLong());
    }
}
