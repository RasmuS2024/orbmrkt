package orbmrkt.payment;

import orbmrkt.dto.ApiResponse;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
import orbmrkt.payment.dto.TopUpRequest;
import orbmrkt.payment.repository.AccountRepository;
import orbmrkt.payment.repository.InboxRepository;
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
class PaymentsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AccountRepository accountRepository;

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
        accountRepository.deleteAll();
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

    private AccountResponse createAccount() {
        var response = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);
        return response.getBody();
    }

    @Test
    void createAccount_success() {
        AccountResponse body = createAccount();

        assertNotNull(body);
        assertEquals(USER_ID, body.getUserId());
        assertEquals(0, body.getBalance());
        assertNotNull(body.getCreatedAt());
    }

    @Test
    void topUp_success() {
        createAccount();

        TopUpRequest topUp = new TopUpRequest();
        topUp.setAmount(500);

        var response = rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(topUp, headers()),
                BalanceResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BalanceResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID, body.getUserId());
        assertEquals(500, body.getBalance());
        assertEquals("geocredits", body.getCurrency());
    }

    @Test
    void getBalance_returnsCurrentBalance() {
        createAccount();

        TopUpRequest topUp = new TopUpRequest();
        topUp.setAmount(500);
        rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(topUp, headers()),
                BalanceResponse.class);

        var response = rest.exchange(
                "/api/v1/payments/accounts/balance",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                BalanceResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BalanceResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getBalance());
    }

    @Test
    void createAccount_existing_returnsExisting() {
        AccountResponse first = createAccount();

        var response = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AccountResponse second = response.getBody();
        assertNotNull(second);
        assertEquals(first.getUserId(), second.getUserId());
        assertEquals(first.getBalance(), second.getBalance());
    }

    @Test
    void createAccount_missingUserId_returns400() {
        var response = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void topUp_accountNotFound_returns404() {
        TopUpRequest topUp = new TopUpRequest();
        topUp.setAmount(500);

        var response = rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(topUp, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ACCOUNT_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void getBalance_accountNotFound_returns404() {
        var response = rest.exchange(
                "/api/v1/payments/accounts/balance",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ACCOUNT_NOT_FOUND", response.getBody().getErrorCode());
    }
}
