package orbmrkt.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.ArchivePayload;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.exception.OrderException;
import orbmrkt.order.messaging.OrderEventPublisher;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<OrderEntity> orderCaptor;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(repository, eventPublisher, objectMapper);
    }

    @Test
    void createOrder_zeroPrice_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.ZERO);
        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));
        assertEquals("INVALID_PRICE", ex.getErrorCode());
    }

    @Test
    void createOrder_serializePayloadFails_throwsException() throws Exception {
        ArchivePayload archive = new ArchivePayload();
        archive.setAoi("area1");
        archive.setCaptureDate("2026-01-01");
        archive.setSensorType("optical");
        when(objectMapper.convertValue(any(), eq(ArchivePayload.class))).thenReturn(archive);
        doThrow(new RuntimeException("Serialization error")).when(objectMapper).writeValueAsString(any(Map.class));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of("aoi", "area1", "capture_date", "2026-01-01", "sensor_type", "optical"));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));
        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_savesRejectedOrderOnValidationFailure() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));

        assertThrows(OrderException.class, () -> orderService.createOrder("user", request));

        verify(repository).save(orderCaptor.capture());
        assertEquals("REJECTED", orderCaptor.getValue().getStatus());
    }
}