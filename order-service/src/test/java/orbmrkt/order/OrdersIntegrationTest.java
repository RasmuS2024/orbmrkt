package orbmrkt.order;

import orbmrkt.dto.ApiResponse;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.test.KafkaTestUtils;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.InboxRepository;
import orbmrkt.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EmbeddedKafka(
        topics = {"order.payment.requested", "order.payment.result"},
        controlledShutdown = true,
        partitions = 1,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("test")
class OrdersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private KafkaTestUtils kafka;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InboxRepository inboxRepository;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        inboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", USER_ID);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders headers(String userId) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", userId);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private OrderResponse createOrder() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(500L);
        request.setPayload(Map.of(
                "aoi", "test-area",
                "capture_date", "2026-01-01",
                "sensor_type", "optical"
        ));
        return rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()), OrderResponse.class).getBody();
    }

    private OrderEntity waitForOrderStatus(UUID orderId, String expectedStatus, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        OrderEntity order;
        do {
            order = orderRepository.findById(orderId).orElse(null);
            if (order != null && expectedStatus.equals(order.getStatus())) {
                return order;
            }
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        return order;
    }

    @Test
    void handlePaymentCompleted_setsStatusPaid() throws Exception {
        OrderResponse order = createOrder();
        var event = new OrderPaymentCompleted();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(order.getOrderId());
        event.setUserId(USER_ID);
        event.setAmount(500L);
        event.setNewBalance(500);
        kafka.send("order.payment.result", event.getOrderId().toString(), event);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAID", 5000);
        assertNotNull(updated);
        assertEquals("PAID", updated.getStatus());
    }

    @Test
    void handlePaymentFailed_setsStatusFailed() throws Exception {
        OrderResponse order = createOrder();
        var event = new OrderPaymentFailed();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(order.getOrderId());
        event.setUserId(USER_ID);
        event.setReason("INSUFFICIENT_BALANCE");
        kafka.send("order.payment.result", event.getOrderId().toString(), event);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAYMENT_FAILED", 5000);
        assertNotNull(updated);
        assertEquals("PAYMENT_FAILED", updated.getStatus());
    }

    @Test
    void duplicateEventId_ignoredByInbox() throws Exception {
        OrderResponse order = createOrder();
        var event = new OrderPaymentCompleted();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(order.getOrderId());
        event.setUserId(USER_ID);
        event.setAmount(500L);
        event.setNewBalance(500);

        kafka.send("order.payment.result", event.getOrderId().toString(), event);
        kafka.send("order.payment.result", event.getOrderId().toString(), event);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAID", 5000);
        assertNotNull(updated);
        assertEquals("PAID", updated.getStatus());
    }

    @Test
    void createOrder_missingUserId_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(500L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_invalidPayload_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(500L);
        request.setPayload(Map.of());

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PAYLOAD", response.getBody().getErrorCode());
    }

    @Test
    void getOrder_notFound_returns404() {
        var response = rest.exchange("/api/v1/orders/orders/" + UUID.randomUUID(),
                HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void getOrder_missingUserId_returns400() {
        var response = rest.exchange("/api/v1/orders/orders/" + UUID.randomUUID(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_invalidUserId_returns400() {
        var h = new HttpHeaders();
        h.set("X-User-Id", "bad-uuid");
        h.setContentType(MediaType.APPLICATION_JSON);
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(500L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, h),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void listOrders_returnsOnlyUserOrders() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(300L);
        request.setPayload(Map.of("aoi", "other-area", "capture_date", "2026-02-01", "sensor_type", "sar"));
        rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers("00000000-0000-0000-0000-000000000001")), OrderResponse.class);

        OrderResponse order1 = createOrder();
        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<OrderResponse>>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(order1.getOrderId(), response.getBody().get(0).getOrderId());
    }

    @Test
    void createOrder_invalidJson_returns400() {
        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>("not json", headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_JSON", response.getBody().getErrorCode());
    }

    @Test
    void requestNonExistentEndpoint_returns404() {
        var response = rest.exchange(
                "/api/v1/orders/non-existent",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_zeroPrice_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(0L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PRICE", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_negativePrice_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(-100L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PRICE", response.getBody().getErrorCode());
    }

    @Test
    void getOrder_wrongUser_returns404() {
        var order = createOrder();
        var response = rest.exchange("/api/v1/orders/orders/" + order.getOrderId(),
                HttpMethod.GET, new HttpEntity<>(headers("00000000-0000-0000-0000-000000000001")),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_taskingValid_returns201() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.TASKING);
        request.setPrice(1000L);
        request.setPayload(Map.of(
                "aoi", "tasking-area",
                "time_window", Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-01-02T00:00:00Z"),
                "sensor_type", "sar"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                OrderResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAYMENT_PENDING", response.getBody().getStatus());
        assertEquals(ProductType.TASKING.name(), response.getBody().getProductType());
    }

    @Test
    void createOrder_monitoringValid_returns201() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.MONITORING);
        request.setPrice(1500L);
        request.setPayload(Map.of(
                "aoi", "monitoring-area",
                "cadence", "daily",
                "duration_days", 30,
                "sensor_type", "optical"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                OrderResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAYMENT_PENDING", response.getBody().getStatus());
        assertEquals(ProductType.MONITORING.name(), response.getBody().getProductType());
    }

    @Test
    void createOrder_taskingInvalidPayload_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.TASKING);
        request.setPrice(500L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PAYLOAD", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_monitoringInvalidPayload_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.MONITORING);
        request.setPrice(500L);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange("/api/v1/orders/orders", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PAYLOAD", response.getBody().getErrorCode());
    }
}
