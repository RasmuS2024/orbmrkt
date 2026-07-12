package orbmrkt.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.order.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventPublisher publisher;

    @Test
    void publishPaymentRequested_serializationFails_throwsException() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        var event = new OrderPaymentRequested();
        event.setOrderId(UUID.randomUUID());
        event.setUserId("user");
        event.setAmount(BigDecimal.valueOf(500));

        assertThrows(RuntimeException.class, () -> publisher.publishPaymentRequested(event));

        verify(outboxRepository, never()).save(any());
    }
}
