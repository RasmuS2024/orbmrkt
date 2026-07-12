package orbmrkt.order;

import orbmrkt.dto.ApiResponse;
import orbmrkt.dto.OrderPaymentCompleted;
import orbmrkt.dto.OrderPaymentFailed;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.messaging.KafkaTestUtils;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.InboxRepository;
import orbmrkt.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

    private static final String USER_ID = "test-user-1";

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
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of(
                "aoi", "test-area",
                "capture_date", "2026-01-01",
                "sensor_type", "optical"
        ));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                OrderResponse.class);
        return response.getBody();
    }

    private OrderEntity waitForOrderStatus(UUID orderId, String expectedStatus, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        OrderEntity order;
        do {
            order = orderRepository.findById(orderId).orElse(null);
            if (order != null && expectedStatus.equals(order.getStatus())) return order;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        return order;
    }

    @Test
    void handlePaymentCompleted_setsStatusPaid() throws Exception {
        OrderResponse order = createOrder();
        assertEquals("PAYMENT_PENDING", order.getStatus(),
                "После создания заказ должен быть в статусе PAYMENT_PENDING");

        var completedEvent = new OrderPaymentCompleted();
        completedEvent.setEventId(UUID.randomUUID());
        completedEvent.setOrderId(order.getOrderId());
        completedEvent.setUserId(USER_ID);
        completedEvent.setAmount(BigDecimal.valueOf(500));
        completedEvent.setNewBalance(500);

        kafka.send("order.payment.result", completedEvent.getOrderId().toString(), completedEvent);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAID", 5000);
        assertNotNull(updated, "Заказ должен быть PAID");
        assertEquals("PAID", updated.getStatus());
        assertNull(updated.getFailureReason(),
                "При успешной оплате failureReason должен быть null");
    }

    @Test
    void handlePaymentFailed_setsStatusFailed() throws Exception {
        OrderResponse order = createOrder();

        var failedEvent = new OrderPaymentFailed();
        failedEvent.setEventId(UUID.randomUUID());
        failedEvent.setOrderId(order.getOrderId());
        failedEvent.setUserId(USER_ID);
        failedEvent.setReason("INSUFFICIENT_BALANCE");

        kafka.send("order.payment.result", failedEvent.getOrderId().toString(), failedEvent);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAYMENT_FAILED", 5000);
        assertNotNull(updated, "Заказ должен быть PAYMENT_FAILED");
        assertEquals("PAYMENT_FAILED", updated.getStatus());
        assertEquals("INSUFFICIENT_BALANCE", updated.getFailureReason(),
                "Должен сохраниться failureReason из события");
    }

    @Test
    void duplicateEventId_ignoredByInbox() throws Exception {
        OrderResponse order = createOrder();

        var completedEvent = new OrderPaymentCompleted();
        UUID eventId = UUID.randomUUID();
        completedEvent.setEventId(eventId);
        completedEvent.setOrderId(order.getOrderId());
        completedEvent.setUserId(USER_ID);
        completedEvent.setAmount(BigDecimal.valueOf(500));
        completedEvent.setNewBalance(500);

        kafka.send("order.payment.result", completedEvent.getOrderId().toString(), completedEvent);
        kafka.send("order.payment.result", completedEvent.getOrderId().toString(), completedEvent);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAID", 5000);
        assertNotNull(updated);
        assertEquals("PAID", updated.getStatus());
    }

    @Test
    void duplicatePaymentFailedEvent_ignoredByInbox() throws Exception {
        OrderResponse order = createOrder();

        var failedEvent = new OrderPaymentFailed();
        UUID eventId = UUID.randomUUID();
        failedEvent.setEventId(eventId);
        failedEvent.setOrderId(order.getOrderId());
        failedEvent.setUserId(USER_ID);
        failedEvent.setReason("INSUFFICIENT_BALANCE");

        kafka.send("order.payment.result", failedEvent.getOrderId().toString(), failedEvent);
        kafka.send("order.payment.result", failedEvent.getOrderId().toString(), failedEvent);

        OrderEntity updated = waitForOrderStatus(order.getOrderId(), "PAYMENT_FAILED", 5000);
        assertNotNull(updated);
        assertEquals("PAYMENT_FAILED", updated.getStatus());
    }

    @Test
    void createOrder_missingUserId_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_invalidPrice_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.ZERO);
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PRICE", response.getBody().getErrorCode());
    }

    @Test
    void createOrder_unknownProductType_returns400() {
        // case 1: null (field not sent)
        var request = new CreateOrderRequest();
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of("aoi", "test"));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("UNKNOWN_PRODUCT_TYPE", response.getBody().getErrorCode());

        // case 2: invalid string value
        String invalidBody = """
                {"product_type": "BOGUS", "price": 500, "payload": {"aoi": "test"}}
                """;
        var headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response2 = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(invalidBody, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertEquals("UNKNOWN_PRODUCT_TYPE", response2.getBody().getErrorCode());
    }

    @Test
    void createOrder_invalidPayload_returns400() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of());

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PAYLOAD", response.getBody().getErrorCode());
    }

    @Test
    void getOrder_notFound_returns404() {
        var response = rest.exchange(
                "/api/v1/orders/orders/" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void getOrder_wrongUser_returns404() {
        OrderResponse order = createOrder();

        var response = rest.exchange(
                "/api/v1/orders/orders/" + order.getOrderId(),
                HttpMethod.GET,
                new HttpEntity<>(headers("other-user")),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void listOrders_returnsOnlyUserOrders() {
        // create order for USER_ID
        OrderResponse order1 = createOrder();
        // create order for another user
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(300));
        request.setPayload(Map.of(
                "aoi", "other-area",
                "capture_date", "2026-02-01",
                "sensor_type", "sar"
        ));
        rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers("other-user")),
                OrderResponse.class);

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<OrderResponse>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(order1.getOrderId(), response.getBody().get(0).getOrderId());
    }

    @Test
    void createOrder_success_returns201AndFullBody() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of(
                "aoi", "test-area",
                "capture_date", "2026-01-01",
                "sensor_type", "optical"
        ));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                OrderResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        OrderResponse body = response.getBody();
        assertThat(body.getStatus()).isEqualTo("PAYMENT_PENDING");
        assertThat(body.getProductType()).isEqualTo("ARCHIVE");
        assertThat(body.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertNotNull(body.getOrderId());
        assertNotNull(body.getCreatedAt());
    }

    @Test
    void getOrder_success_returnsOrder() {
        OrderResponse created = createOrder();

        var response = rest.exchange(
                "/api/v1/orders/orders/" + created.getOrderId(),
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        OrderResponse body = response.getBody();
        assertEquals(created.getOrderId(), body.getOrderId());
        assertEquals(created.getStatus(), body.getStatus());
        assertEquals(created.getProductType(), body.getProductType());
        assertThat(body.getPrice()).isEqualByComparingTo(created.getPrice());
        assertNotNull(body.getCreatedAt());
    }

    @Test
    void createOrder_emptyBody_returns400() {
        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getOrder_missingUserId_returns400() {
        var response = rest.exchange(
                "/api/v1/orders/orders/" + UUID.randomUUID(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_USER_ID", response.getBody().getErrorCode());
    }
}
