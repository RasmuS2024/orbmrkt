package orbmrkt.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.ArchivePayload;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.MonitoringPayload;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.dto.TaskingPayload;
import orbmrkt.order.dto.TimeWindow;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void createOrder_nullUserId_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(null, request));

        assertEquals("MISSING_USER_ID", ex.getErrorCode());
    }

    @Test
    void createOrder_blankUserId_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("  ", request));

        assertEquals("MISSING_USER_ID", ex.getErrorCode());
    }

    @Test
    void createOrder_nullProductType_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPrice(BigDecimal.valueOf(500));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("UNKNOWN_PRODUCT_TYPE", ex.getErrorCode());
    }

    @Test
    void createOrder_nullPrice_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PRICE", ex.getErrorCode());
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
    void createOrder_nullPayload_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_emptyPayload_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of());

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_archiveInvalidPayload_throwsException() {
        ArchivePayload archive = new ArchivePayload();
        archive.setAoi(null);
        archive.setCaptureDate("2026-01-01");
        archive.setSensorType("optical");
        when(objectMapper.convertValue(any(), eq(ArchivePayload.class))).thenReturn(archive);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of("aoi", "area1", "capture_date", "2026-01-01", "sensor_type", "optical"));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_archiveSuccess() throws Exception {
        ArchivePayload archive = new ArchivePayload();
        archive.setAoi("area1");
        archive.setCaptureDate("2026-01-01");
        archive.setSensorType("optical");
        when(objectMapper.convertValue(any(), eq(ArchivePayload.class))).thenReturn(archive);
        doReturn("{\"key\":\"value\"}").when(objectMapper).writeValueAsString(any(Map.class));
        UUID orderId = UUID.randomUUID();
        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(orderId);
            }
            return entity;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of("aoi", "area1", "capture_date", "2026-01-01", "sensor_type", "optical"));

        OrderResponse response = orderService.createOrder("user", request);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals("PAYMENT_PENDING", response.getStatus());
        assertEquals("ARCHIVE", response.getProductType());
        assertEquals(BigDecimal.valueOf(500), response.getPrice());
        verify(eventPublisher).publishPaymentRequested(any());
    }

    @Test
    void createOrder_taskingInvalidPayload_throwsException() {
        TaskingPayload tasking = new TaskingPayload();
        tasking.setAoi(null);
        tasking.setTimeWindow(new TimeWindow());
        tasking.getTimeWindow().setFrom("2026-01-01");
        tasking.getTimeWindow().setTo("2026-01-02");
        tasking.setSensorType("optical");
        when(objectMapper.convertValue(any(), eq(TaskingPayload.class))).thenReturn(tasking);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.TASKING);
        request.setPrice(BigDecimal.valueOf(1000));
        request.setPayload(Map.of("aoi", "area1", "time_window", Map.of("from", "2026-01-01", "to", "2026-01-02"), "sensor_type", "optical"));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_taskingSuccess() throws Exception {
        TaskingPayload tasking = new TaskingPayload();
        tasking.setAoi("area1");
        TimeWindow tw = new TimeWindow();
        tw.setFrom("2026-01-01");
        tw.setTo("2026-01-02");
        tasking.setTimeWindow(tw);
        tasking.setSensorType("optical");
        when(objectMapper.convertValue(any(), eq(TaskingPayload.class))).thenReturn(tasking);
        doReturn("{}").when(objectMapper).writeValueAsString(any(Map.class));
        UUID orderId = UUID.randomUUID();
        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(orderId);
            }
            return entity;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.TASKING);
        request.setPrice(BigDecimal.valueOf(1000));
        request.setPayload(Map.of("aoi", "area1", "time_window", Map.of("from", "2026-01-01", "to", "2026-01-02"), "sensor_type", "optical"));

        OrderResponse response = orderService.createOrder("user", request);

        assertEquals("PAYMENT_PENDING", response.getStatus());
        verify(eventPublisher).publishPaymentRequested(any());
    }

    @Test
    void createOrder_monitoringInvalidPayload_throwsException() {
        MonitoringPayload monitoring = new MonitoringPayload();
        monitoring.setAoi("area1");
        monitoring.setCadence("daily");
        monitoring.setDurationDays(0);
        when(objectMapper.convertValue(any(), eq(MonitoringPayload.class))).thenReturn(monitoring);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.MONITORING);
        request.setPrice(BigDecimal.valueOf(2000));
        request.setPayload(Map.of("aoi", "area1", "cadence", "daily", "duration_days", 0));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        assertEquals("INVALID_PAYLOAD", ex.getErrorCode());
    }

    @Test
    void createOrder_monitoringSuccess() throws Exception {
        MonitoringPayload monitoring = new MonitoringPayload();
        monitoring.setAoi("area1");
        monitoring.setCadence("daily");
        monitoring.setDurationDays(30);
        when(objectMapper.convertValue(any(), eq(MonitoringPayload.class))).thenReturn(monitoring);
        doReturn("{}").when(objectMapper).writeValueAsString(any(Map.class));
        UUID orderId = UUID.randomUUID();
        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(orderId);
            }
            return entity;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.MONITORING);
        request.setPrice(BigDecimal.valueOf(2000));
        request.setPayload(Map.of("aoi", "area1", "cadence", "daily", "duration_days", 30));

        OrderResponse response = orderService.createOrder("user", request);

        assertEquals("PAYMENT_PENDING", response.getStatus());
        verify(eventPublisher).publishPaymentRequested(any());
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
    void getOrder_existing_returnsOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setUserId("user");
        entity.setProductType("ARCHIVE");
        entity.setPrice(BigDecimal.valueOf(500));
        entity.setStatus("PAYMENT_PENDING");
        when(repository.findById(orderId)).thenReturn(Optional.of(entity));

        OrderResponse response = orderService.getOrder(orderId, "user");

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals("PAYMENT_PENDING", response.getStatus());
    }

    @Test
    void getOrder_notFound_throwsException() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.getOrder(orderId, "user"));

        assertEquals("ORDER_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void getOrder_wrongUser_throwsException() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setUserId("other-user");
        when(repository.findById(orderId)).thenReturn(Optional.of(entity));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.getOrder(orderId, "user"));

        assertEquals("ORDER_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void listOrders_returnsUserOrders() {
        OrderEntity entity = new OrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId("user");
        entity.setProductType("ARCHIVE");
        entity.setPrice(BigDecimal.valueOf(500));
        entity.setStatus("PAID");
        when(repository.findAllByUserId("user")).thenReturn(List.of(entity));

        List<OrderResponse> orders = orderService.listOrders("user");

        assertEquals(1, orders.size());
        assertEquals("PAID", orders.getFirst().getStatus());
    }

    @Test
    void createOrder_savesRejectedOrderOnValidationFailure() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));

        assertThrows(OrderException.class,
                () -> orderService.createOrder("user", request));

        verify(repository).save(orderCaptor.capture());
        OrderEntity saved = orderCaptor.getValue();
        assertEquals("REJECTED", saved.getStatus());
        assertEquals("INVALID_PAYLOAD", saved.getFailureReason());
    }
}
