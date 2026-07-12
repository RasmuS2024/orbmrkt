package orbmrkt.order.messaging;

import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.order.model.InboxEntity;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.InboxRepository;
import orbmrkt.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InboxRepository inboxRepository;

    @InjectMocks
    private OrderEventConsumer consumer;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @Test
    void handlePaymentCompleted_concurrentDuplicate_skips() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        doThrow(DataIntegrityViolationException.class).when(inboxRepository).save(any(InboxEntity.class));

        var event = new OrderPaymentCompleted();
        event.setEventId(eventId);
        event.setOrderId(orderId);
        event.setUserId("user");
        event.setAmount(BigDecimal.valueOf(500));
        event.setNewBalance(500);

        consumer.handlePaymentCompleted(event);

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void handlePaymentFailed_duplicateEvent_skips() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(true);

        var event = new OrderPaymentFailed();
        event.setEventId(eventId);
        event.setOrderId(orderId);
        event.setUserId("user");
        event.setReason("INSUFFICIENT_BALANCE");

        consumer.handlePaymentFailed(event);

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void handlePaymentFailed_concurrentDuplicate_skips() {
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        doThrow(DataIntegrityViolationException.class).when(inboxRepository).save(any(InboxEntity.class));

        var event = new OrderPaymentFailed();
        event.setEventId(eventId);
        event.setOrderId(orderId);
        event.setUserId("user");
        event.setReason("INSUFFICIENT_BALANCE");

        consumer.handlePaymentFailed(event);

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void handleUnknown_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.handleUnknown("random message"));
    }
}
