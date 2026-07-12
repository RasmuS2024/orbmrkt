package orbmrkt.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.payment.config.OutboxProperties;
import orbmrkt.payment.model.OutboxEntity;
import orbmrkt.payment.repository.InboxRepository;
import orbmrkt.payment.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollingWorkerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPollingWorker worker;

    @Captor
    private ArgumentCaptor<OutboxEntity> outboxCaptor;

    private final UUID eventId = UUID.randomUUID();

    @Test
    void pollAndPublish_processesPendingEvents() throws Exception {
        OutboxEntity event = new OutboxEntity(eventId, "order.payment.result", "{}", OrderPaymentCompleted.class.getName());
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtIsNull(anyInt(), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtBefore(anyInt(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(properties.getMaxAttempts()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);
        when(objectMapper.readValue("{}", OrderPaymentCompleted.class)).thenReturn(new OrderPaymentCompleted());
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        worker.pollAndPublish();

        verify(outboxRepository).save(outboxCaptor.capture());
        assertTrue(outboxCaptor.getValue().isProcessed());
    }

    @Test
    void pollAndPublish_skipsNullEventType() {
        OutboxEntity event = new OutboxEntity();
        event.setEventId(eventId);
        event.setTopic("order.payment.result");
        event.setPayload("{}");
        event.setEventType(null);
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtIsNull(anyInt(), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtBefore(anyInt(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(properties.getMaxAttempts()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);

        worker.pollAndPublish();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void pollAndPublish_retryOnFailure() throws Exception {
        OutboxEntity event = new OutboxEntity(eventId, "order.payment.result", "{}", OrderPaymentCompleted.class.getName());
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtIsNull(anyInt(), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(outboxRepository.findByProcessedFalseAndAttemptsLessThanAndNextRetryAtBefore(anyInt(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(properties.getMaxAttempts()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);
        when(objectMapper.readValue("{}", OrderPaymentCompleted.class)).thenReturn(new OrderPaymentCompleted());
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("Kafka unavailable"));

        worker.pollAndPublish();

        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEntity saved = outboxCaptor.getValue();
        assertEquals(1, saved.getAttempts());
        assertEquals("Kafka unavailable", saved.getLastError());
        assertNotNull(saved.getNextRetryAt());
        assertFalse(saved.isProcessed());
    }

    @Test
    void cleanup_deletesOldRecords() {
        when(properties.getCleanupDays()).thenReturn(7);

        worker.cleanup();

        verify(outboxRepository).deleteByProcessedTrueAndCreatedAtBefore(any(Instant.class));
        verify(inboxRepository).deleteByProcessedAtBefore(any(Instant.class));
    }
}
