package com.tenpo.challenge.calculation;

import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource(properties = "percentage-provider.failure-mode=client-error")
class CalculationClientErrorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RetryRegistry retryRegistry;

    @Test
    void failsImmediatelyAndReturns502() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isEqualTo(502)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(retryRegistry.retry("percentageProvider").getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
            .isEqualTo(1L);
    }
}
