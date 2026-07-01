package orbmrkt.order.service;

import lombok.RequiredArgsConstructor;
import orbmrkt.dto.OrderPaymentRequestedEvent;
import orbmrkt.dto.OrderStatus;
import orbmrkt.dto.PaymentStatus;
import orbmrkt.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.PENDING);
        order = repository.save(order);

        OrderPaymentRequestedEvent event = new OrderPaymentRequestedEvent();
        event.setOrderId(order.getId());
        event.setAmount(order.getAmount());
        eventPublisher.publish(event);

        return order;
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, PaymentStatus status) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(status == PaymentStatus.COMPLETED ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
        repository.save(order);
    }
}
