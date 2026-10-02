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
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class HistoryPaginationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void firstPageHasNextAndSecondPageDoesNot() {
        IntStream.range(0, 25).forEach(i -> webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isOk());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(20)
                .jsonPath("$.hasNext").isEqualTo(true));

        webTestClient.get().uri("/api/v1/history?page=1&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").value(Integer.class, greaterThanOrEqualTo(5))
            .jsonPath("$.content.length()").value(Integer.class, lessThanOrEqualTo(20))
            .jsonPath("$.hasNext").isEqualTo(false);
    }
}
