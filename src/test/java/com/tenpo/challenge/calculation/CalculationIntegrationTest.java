package com.tenpo.challenge.calculation;

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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CalculationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returns200WithComputedResult() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result").isEqualTo(11);
    }

    @Test
    void returns400WithTwoErrorsWhenBodyIsEmpty() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(2);
    }

    @Test
    void returns400WhenFieldIsMissing() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.errors").isArray();
    }

    @Test
    void returns400WhenFieldIsNonNumeric() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": \"abc\", \"num2\": 5}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.errors").isArray();
    }
}
