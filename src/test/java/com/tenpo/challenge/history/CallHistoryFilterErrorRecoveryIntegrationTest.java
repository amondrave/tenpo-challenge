package com.tenpo.challenge.history;

import com.tenpo.challenge.shared.web.FilterOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(CallHistoryFilterErrorRecoveryIntegrationTest.ThrowingFilterConfig.class)
class CallHistoryFilterErrorRecoveryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void errorEscapingAfterHistoryFilterIsRecordedAsSanitized500() {
        EntityExchangeResult<byte[]> result = webTestClient.get().uri(ThrowingFilterConfig.BOOM_PATH)
            .exchange()
            .expectStatus().isEqualTo(500)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").isEqualTo("Internal Server Error")
            .returnResult();

        String body = new String(result.getResponseBodyContent(), StandardCharsets.UTF_8);
        assertThat(body).doesNotContain(ThrowingFilterConfig.LEAK_CANARY);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            webTestClient.get().uri("/api/v1/history?page=0&size=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.path=='" + ThrowingFilterConfig.BOOM_PATH + "')]").exists()
                .jsonPath("$.content[?(@.path=='" + ThrowingFilterConfig.BOOM_PATH + "')].responseStatus")
                .isEqualTo(500));
    }

    @TestConfiguration
    static class ThrowingFilterConfig {

        static final String BOOM_PATH = "/api/v1/boom";
        static final String LEAK_CANARY = "simulated-internal-failure-detail";

        @Bean
        @Order(FilterOrder.CALL_HISTORY + 1)
        WebFilter throwingWebFilter() {
            return new ThrowingWebFilter();
        }

        private static class ThrowingWebFilter implements WebFilter {

            @Override
            public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                return Mono.just(exchange.getRequest().getPath().value())
                    .filter(BOOM_PATH::equals)
                    .<Mono<Void>>map(path -> Mono.error(new RuntimeException(LEAK_CANARY)))
                    .defaultIfEmpty(Mono.defer(() -> chain.filter(exchange)))
                    .flatMap(Function.identity());
            }
        }
    }
}
