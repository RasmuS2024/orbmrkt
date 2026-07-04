package orbmrkt.payment;

import orbmrkt.dto.ApiResponse;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.dto.TopUpRequest;
import orbmrkt.payment.messaging.KafkaTestUtils;
import orbmrkt.payment.repository.AccountRepository;
import orbmrkt.payment.repository.ProcessedPaymentRepository;
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
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EmbeddedKafka(
        topics = {"order.payment.requested", "order.payment.result"},
        controlledShutdown = true,
        partitions = 1,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("test")
class PaymentsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private KafkaTestUtils kafka;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProcessedPaymentRepository processedPaymentRepository;

    private String userId;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        processedPaymentRepository.deleteAll();
        accountRepository.deleteAll();
        userId = UUID.randomUUID().toString();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", userId);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private void createAccount() {
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});
    }

    private void topUp(long amount) {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(amount);
        rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<ApiResponse<BalanceResponse>>() {});
    }

    private long getBalance() {
        var response = rest.exchange(
                "/api/v1/payments/accounts/balance",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<BalanceResponse>>() {});
        return response.getBody().getData().getBalance();
    }

    private void publishPaymentRequested(UUID orderId, long amount) {
        var event = new OrderPaymentRequested();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setAmount(BigDecimal.valueOf(amount));
        event.setOccurredAt(Instant.now());
        kafka.send("order.payment.requested", event);
    }

    private long waitForBalanceChange(long expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        long balance;
        do {
            balance = getBalance();
            if (balance == expected) return balance;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        return balance;
    }

    @Test
    void scenario1_happyPath() throws Exception {
        createAccount();
        topUp(1000);
        UUID orderId = UUID.randomUUID();

        publishPaymentRequested(orderId, 120);

        long balance = waitForBalanceChange(880, 5000);
        assertEquals(880, balance, "Баланс должен быть 1000 - 120 = 880");
    }

    @Test
    void scenario2_insufficientFunds() throws Exception {
        createAccount();
        topUp(50);
        UUID orderId = UUID.randomUUID();

        publishPaymentRequested(orderId, 120);

        Thread.sleep(1000);
        long balance = getBalance();
        assertEquals(50, balance, "Баланс должен остаться 50 — недостаточно средств");
    }

    @Test
    void scenario3_idempotentDuplicateOrderId() throws Exception {
        createAccount();
        topUp(1000);
        UUID orderId = UUID.randomUUID();

        publishPaymentRequested(orderId, 120);
        publishPaymentRequested(orderId, 120);

        long balance = waitForBalanceChange(880, 5000);
        assertEquals(880, balance, "Баланс должен быть 880 — повторный платёж не должен списывать");
    }

    @Test
    void scenario4_twoOrders() throws Exception {
        createAccount();
        topUp(1000);

        publishPaymentRequested(UUID.randomUUID(), 400);
        publishPaymentRequested(UUID.randomUUID(), 400);

        long balance = waitForBalanceChange(200, 5000);
        assertEquals(200, balance, "Баланс должен быть 1000 - 400 - 400 = 200");
    }

    @Test
    void scenario5_duplicateAccountCreation() {
        var first = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});

        var second = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {});

        assertEquals(HttpStatus.CREATED, first.getStatusCode(),
                "Первый запрос — 201 Created");
        assertEquals(HttpStatus.OK, second.getStatusCode(),
                "Повторный запрос — 200 OK (идемпотентность)");

        assertEquals(1, accountRepository.count(),
                "Должен быть только один счёт для user_id");
    }
}
