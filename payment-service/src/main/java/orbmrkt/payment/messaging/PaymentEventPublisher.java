package orbmrkt.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.payment.model.OutboxEntity;
import orbmrkt.payment.repository.OutboxRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PAYMENT_RESULT = "order.payment.result";

    public void publishPaymentCompleted(UUID orderId, String userId, BigDecimal amount, long newBalance) {
        OrderPaymentCompleted event = new OrderPaymentCompleted();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setAmount(amount);
        event.setNewBalance(newBalance);

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = new OutboxEntity(event.getEventId(), TOPIC_PAYMENT_RESULT, payload, event.getClass().getName());
            outboxRepository.save(outbox);
            log.info("Payment completed event saved to outbox: eventId={}, orderId={}", event.getEventId(), orderId);
        } catch (Exception e) {
            log.error("Failed to serialize payment completed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    public void publishPaymentFailed(UUID orderId, String userId, String reason) {
        OrderPaymentFailed event = new OrderPaymentFailed();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setReason(reason);

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = new OutboxEntity(event.getEventId(), TOPIC_PAYMENT_RESULT, payload, event.getClass().getName());
            outboxRepository.save(outbox);
            log.info("Payment failed event saved to outbox: eventId={}, orderId={}", event.getEventId(), orderId);
        } catch (Exception e) {
            log.error("Failed to serialize payment failed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
}
