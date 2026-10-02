package com.tenpo.challenge.shared.web;

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
class UnknownRouteIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void unknownRouteReturns404WithProblemDetailsAndIsRecorded() {
        webTestClient.get().uri("/api/v1/unknown")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").exists()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.instance").isEqualTo("/api/v1/unknown");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.path=='/api/v1/unknown')]").exists()
                .jsonPath("$.content[?(@.path=='/api/v1/unknown')].responseStatus").isEqualTo(404));
    }

    @Test
    void unsupportedMethodOnKnownRouteReturns405AndIsRecorded() {
        webTestClient.post().uri("/api/v1/history")
            .exchange()
            .expectStatus().isEqualTo(405)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(405);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.path=='/api/v1/history' && @.method=='POST')]").exists()
                .jsonPath("$.content[?(@.path=='/api/v1/history' && @.method=='POST')].responseStatus").isEqualTo(405));
    }
}
