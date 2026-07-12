package orbmrkt.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.payment.config.OutboxProperties;
import orbmrkt.payment.model.OutboxEntity;
import orbmrkt.payment.repository.InboxRepository;
import orbmrkt.payment.repository.OutboxRepository;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingWorker {

    private static final Map<String, Class<?>> EVENT_TYPE_MAP = Map.of(
            "orbmrkt.dto.OrderPaymentRequested", OrderPaymentRequested.class,
            "orbmrkt.dto.OrderPaymentCompleted", OrderPaymentCompleted.class,
            "orbmrkt.dto.OrderPaymentFailed", OrderPaymentFailed.class
    );

    private final OutboxRepository outboxRepository;
    private final InboxRepository inboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxProperties properties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.polling-interval-ms}")
    @Transactional
    public void pollAndPublish() {
        Instant now = Instant.now();

        List<OutboxEntity> events = outboxRepository
                .findByProcessedFalseAndAttemptsLessThanAndNextRetryAtIsNull(
                        properties.getMaxAttempts(),
                        PageRequest.of(0, properties.getBatchSize()));

        events.addAll(outboxRepository
                .findByProcessedFalseAndAttemptsLessThanAndNextRetryAtBefore(
                        properties.getMaxAttempts(), now,
                        PageRequest.of(0, properties.getBatchSize())));

        for (OutboxEntity event : events) {
            if (event.getEventType() == null) {
                log.warn("Skipping outbox row with null eventType: eventId={}", event.getEventId());
                continue;
            }
            try {
                Class<?> type = EVENT_TYPE_MAP.get(event.getEventType());
                if (type == null) {
                    log.warn("Unknown event type: eventId={}, eventType={}", event.getEventId(), event.getEventType());
                    continue;
                }
                Object payload = objectMapper.readValue(event.getPayload(), type);
                kafkaTemplate.send(event.getTopic(), event.getEventId().toString(), payload).get();
                event.setProcessed(true);
                log.debug("Published event from outbox: eventId={}", event.getEventId());
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
                long delay = (long) Math.pow(2, event.getAttempts()) * 1000;
                event.setNextRetryAt(now.plusMillis(delay));
                log.warn("Failed to publish event: eventId={}, attempts={}, error={}",
                        event.getEventId(), event.getAttempts(), e.getMessage());
            }
            outboxRepository.save(event);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(properties.getCleanupDays()));
        outboxRepository.deleteByProcessedTrueAndCreatedAtBefore(cutoff);
        inboxRepository.deleteByProcessedAtBefore(cutoff);
        log.info("Cleanup completed for records older than {} days", properties.getCleanupDays());
    }
}
