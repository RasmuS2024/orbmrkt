package orbmrkt.order.messaging;

import lombok.RequiredArgsConstructor;
import orbmrkt.dto.OrderPaymentRequestedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT_REQUESTED = "order.payment.requested";

    public void publishPaymentRequested(OrderPaymentRequestedEvent event) {
        kafkaTemplate.send(TOPIC_PAYMENT_REQUESTED, event.getOrderId().toString(), event);
    }
}
