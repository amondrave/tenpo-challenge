package com.tenpo.challenge.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackProblemWebExceptionHandlerTest {

    private final FallbackProblemWebExceptionHandler handler =
        new FallbackProblemWebExceptionHandler(new ProblemDetailWriter(new ObjectMapper()));

    @Test
    void writesSanitized500ForEscapedException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/anything").build());

        StepVerifier.create(handler.handle(exchange, new RuntimeException("secret-detail")))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).doesNotContain("secret-detail");
        assertThat(body).contains("\"title\":\"Internal Server Error\"");
    }
}
