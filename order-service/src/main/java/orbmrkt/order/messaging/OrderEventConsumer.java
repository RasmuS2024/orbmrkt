package orbmrkt.order.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.order.model.InboxEntity;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.InboxRepository;
import orbmrkt.order.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "order.payment.result", groupId = "order-service")
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final InboxRepository inboxRepository;

    @Transactional
    @KafkaHandler
    public void handlePaymentCompleted(OrderPaymentCompleted event) {
        if (inboxRepository.existsByEventId(event.getEventId())) {
            log.debug("Duplicate event skipped: eventId={}", event.getEventId());
            return;
        }
        inboxRepository.save(new InboxEntity(event.getEventId()));

        log.info("Получен результат оплаты (COMPLETED) для orderId: {}, newBalance: {}",
                event.getOrderId(), event.getNewBalance());

        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + event.getOrderId()));

        order.setStatus("PAID");
        order.setFailureReason(null);
        log.info("Заказ {} оплачен", event.getOrderId());
    }

    @Transactional
    @KafkaHandler
    public void handlePaymentFailed(OrderPaymentFailed event) {
        if (inboxRepository.existsByEventId(event.getEventId())) {
            log.debug("Duplicate event skipped: eventId={}", event.getEventId());
            return;
        }
        inboxRepository.save(new InboxEntity(event.getEventId()));

        log.warn("Получен результат оплаты (FAILED) для orderId: {}, reason: {}",
                event.getOrderId(), event.getReason());

        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + event.getOrderId()));

        order.setStatus("PAYMENT_FAILED");
        order.setFailureReason(event.getReason());
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object message) {
        log.warn("Получено неизвестное событие: {}", message);
    }
}
