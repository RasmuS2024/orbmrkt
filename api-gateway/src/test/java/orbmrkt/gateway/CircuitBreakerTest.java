package orbmrkt.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CircuitBreakerTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Test
    void orderServiceDown_fallbackReturns503() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().is5xxServerError();

        webTestClient.get()
                .uri("/api/v1/orders")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().is5xxServerError();

        webTestClient.get()
                .uri("/api/v1/orders")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("Order Service временно недоступен");
    }

    @Test
    void paymentServiceDown_fallbackReturns503() {
        webTestClient.get()
                .uri("/api/v1/payments/accounts/balance")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().is5xxServerError();

        webTestClient.get()
                .uri("/api/v1/payments/accounts/balance")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().is5xxServerError();

        webTestClient.get()
                .uri("/api/v1/payments/accounts/balance")
                .header("X-User-Id", "test-user")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("Payment Service временно недоступен");
    }
}
