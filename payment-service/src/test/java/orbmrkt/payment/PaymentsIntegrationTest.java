package orbmrkt.payment;

import orbmrkt.dto.ApiResponse;
import orbmrkt.payment.dto.AccountResponse;
import orbmrkt.payment.dto.BalanceResponse;
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
        accountRepository.deleteAll();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Id", USER_ID);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void createAccount_success() {
        var response = rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        AccountResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID, body.getUserId());
        assertEquals(0, body.getBalance());
        assertNotNull(body.getCreatedAt());
    }

    @Test
    void topUp_success() {
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        var topUp = new orbmrkt.payment.dto.TopUpRequest();
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
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        var topUp = new orbmrkt.payment.dto.TopUpRequest();
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
    void topUp_invalidAmount_returns400() {
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        var topUp = new orbmrkt.payment.dto.TopUpRequest();
        topUp.setAmount(0);

        var response = rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(topUp, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_AMOUNT", response.getBody().getErrorCode());
    }

    @Test
    void getBalance_accountNotFound_returns404() {
        var headers = new HttpHeaders();
        headers.set("X-User-Id", "00000000-0000-0000-0000-000000000001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = rest.exchange(
                "/api/v1/payments/accounts/balance",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ACCOUNT_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void createAccount_invalidUserId_returns400() {
        var h = new HttpHeaders();
        h.set("X-User-Id", "bad-uuid");
        h.setContentType(MediaType.APPLICATION_JSON);

        var response = rest.exchange("/api/v1/payments/accounts", HttpMethod.POST,
                new HttpEntity<>(h),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_USER_ID", response.getBody().getErrorCode());
    }

    @Test
    void topUp_invalidJson_returns400() {
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        var response = rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>("not json", headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_JSON", response.getBody().getErrorCode());
    }

    @Test
    void requestNonExistentEndpoint_returns404() {
        var response = rest.exchange(
                "/api/v1/payments/non-existent",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void topUp_negativeAmount_returns400() {
        rest.exchange(
                "/api/v1/payments/accounts",
                HttpMethod.POST,
                new HttpEntity<>(headers()),
                AccountResponse.class);

        var topUp = new orbmrkt.payment.dto.TopUpRequest();
        topUp.setAmount(-100);

        var response = rest.exchange(
                "/api/v1/payments/accounts/top-up",
                HttpMethod.POST,
                new HttpEntity<>(topUp, headers()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_AMOUNT", response.getBody().getErrorCode());
    }
}
