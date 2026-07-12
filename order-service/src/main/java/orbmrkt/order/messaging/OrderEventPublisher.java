package orbmrkt.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.order.model.OutboxEntity;
import orbmrkt.order.repository.OutboxRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PAYMENT_REQUESTED = "order.payment.requested";

    public void publishPaymentRequested(OrderPaymentRequested event) {
        event.setEventId(UUID.randomUUID());
        event.setOccurredAt(Instant.now());

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = new OutboxEntity(event.getEventId(), TOPIC_PAYMENT_REQUESTED,
                    payload, event.getClass().getName());
            outboxRepository.save(outbox);
            log.info("Event saved to outbox: eventId={}, orderId={}", event.getEventId(), event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to serialize event for outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
}
