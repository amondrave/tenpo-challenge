package com.tenpo.challenge.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void fourthRequestIsRejectedAndRecordedInHistory() {
        for (int i = 0; i < 3; i++) {
            webTestClient.post().uri("/api/v1/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"num1\": 5, \"num2\": 5}")
                .exchange()
                .expectStatus().isOk();
        }

        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectHeader().exists("Retry-After")
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.responseStatus==429)]").exists());
    }

    @Test
    void actuatorHealthDoesNotConsumeRateLimitTokens() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        }

        for (int i = 0; i < 3; i++) {
            webTestClient.post().uri("/api/v1/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"num1\": 5, \"num2\": 5}")
                .exchange()
                .expectStatus().isOk();
        }
    }
}
