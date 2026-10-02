package com.tenpo.challenge.history;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class HistoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void calculateCallAppearsInHistoryWithStatusAndBody() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isOk();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.path=='/api/v1/calculate')]").exists()
                .jsonPath("$.content[?(@.path=='/api/v1/calculate')].responseStatus").isEqualTo(200));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.path=='/api/v1/history')]").exists());
    }

    @Test
    void validationFailureAppearsInHistory() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.responseStatus==400)]").exists());
    }

    @Test
    void oversizedBodyReturns413AndIsRecorded() {
        String padding = "a".repeat(20_000);
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5, \"padding\": \"" + padding + "\"}")
            .exchange()
            .expectStatus().isEqualTo(413);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.responseStatus==413)]").exists());
    }

    @Test
    void invalidSizeReturns400() {
        webTestClient.get().uri("/api/v1/history?page=0&size=0")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void invalidPageReturns400() {
        webTestClient.get().uri("/api/v1/history?page=-1&size=20")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
