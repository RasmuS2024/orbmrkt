package orbmrkt.payment.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.payment.exception.PaymentException;
import orbmrkt.payment.model.InboxEntity;
import orbmrkt.payment.repository.InboxRepository;
import orbmrkt.payment.service.AccountService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final AccountService accountService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final InboxRepository inboxRepository;

    private static final String TOPIC_PAYMENT_REQUESTED = "order.payment.requested";

    @Transactional
    @KafkaListener(topics = TOPIC_PAYMENT_REQUESTED, groupId = "payment-service")
    public void handlePaymentRequested(OrderPaymentRequested event) {
        if (inboxRepository.existsByEventId(event.getEventId())) {
            log.debug("Duplicate event skipped: eventId={}", event.getEventId());
            return;
        }
        inboxRepository.save(new InboxEntity(event.getEventId()));

        log.info("Получен запрос на оплату для orderId: {}, userId: {}, amount: {}",
                event.getOrderId(), event.getUserId(), event.getAmount());

        try {
            long newBalance = accountService.debit(event.getOrderId(), event.getUserId(), event.getAmount().longValue());
            paymentEventPublisher.publishPaymentCompleted(event.getOrderId(), event.getUserId(), event.getAmount(), newBalance);
            log.info("Оплата завершена для orderId: {}", event.getOrderId());
        } catch (PaymentException ex) {
            log.warn("Оплата не удалась для orderId: {}: {}", event.getOrderId(), ex.getMessage());
            paymentEventPublisher.publishPaymentFailed(event.getOrderId(), event.getUserId(), ex.getErrorCode());
        } catch (Exception ex) {
            log.error("Непредвиденная ошибка при обработке оплаты для orderId: {}", event.getOrderId(), ex);
            paymentEventPublisher.publishPaymentFailed(event.getOrderId(), event.getUserId(), "INTERNAL_ERROR");
        }
    }
}
