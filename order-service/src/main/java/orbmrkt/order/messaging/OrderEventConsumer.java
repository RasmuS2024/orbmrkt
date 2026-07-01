package orbmrkt.order.messaging;

import lombok.RequiredArgsConstructor;
import orbmrkt.dto.OrderPaymentResultEvent;
import orbmrkt.dto.PaymentStatus;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(topics = "order.payment.result", groupId = "order-service")
    public void handlePaymentResult(OrderPaymentResultEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        if (event.getStatus() == PaymentStatus.COMPLETED) {
            order.setStatus("PAID");
            order.setFailureReason(null);
        } else {
            order.setStatus("PAYMENT_FAILED");
            order.setFailureReason(event.getFailureReason());
        }
    }
}
