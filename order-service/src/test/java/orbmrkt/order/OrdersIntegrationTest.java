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
import java.util.Map;
import java.util.UUID;

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

    private OrderResponse createOrder() {
        var request = new CreateOrderRequest();
        request.setProductType(ProductType.ARCHIVE);
        request.setPrice(BigDecimal.valueOf(500));
        request.setPayload(Map.of(
                "area_of_interest", "test-area",
                "capture_date", "2026-01-01",
                "sensor_type", "optical"
        ));

        var response = rest.exchange(
                "/api/v1/orders/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<OrderResponse>>() {});
        return response.getBody().getData();
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
}
