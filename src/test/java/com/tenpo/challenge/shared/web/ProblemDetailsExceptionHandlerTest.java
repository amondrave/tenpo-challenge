package com.tenpo.challenge.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsExceptionHandlerTest {

    private final WebTestClient webTestClient = WebTestClient
        .bindToController(new ThrowingTestController())
        .controllerAdvice(new ProblemDetailsExceptionHandler())
        .build();

    @Test
    void missingRequiredFieldReturns400WithErrors() {
        webTestClient.post().uri("/test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").isEqualTo("Validation Failed")
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.errors").isArray();
    }

    @Test
    void nonNumericFieldReturns400WithErrors() {
        webTestClient.post().uri("/test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": \"abc\"}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.errors").isArray();
    }

    @Test
    void methodNotAllowedReturns405() {
        webTestClient.get().uri("/test/validate")
            .exchange()
            .expectStatus().isEqualTo(405)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").isEqualTo("Method Not Allowed")
            .jsonPath("$.instance").isEqualTo("/test/validate");
    }

    @Test
    void notFoundReturns404() {
        webTestClient.post().uri("/test/not-found")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").isEqualTo("Resource Not Found");
    }

    @Test
    void externalServiceUnavailableReturns502() {
        webTestClient.post().uri("/test/external")
            .exchange()
            .expectStatus().isEqualTo(502)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.type").isEqualTo("urn:tenpo:problem:external-service-unavailable");
    }

    @Test
    void unexpectedExceptionReturns500Sanitized() {
        webTestClient.post().uri("/test/unexpected")
            .exchange()
            .expectStatus().isEqualTo(500)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .value(body -> {
                assertThat(body).doesNotContain("hunter2");
                assertThat(body).doesNotContain("IllegalStateException");
            });
    }
}
