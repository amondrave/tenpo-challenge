package com.tenpo.challenge.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitProperties properties = new RateLimitProperties(3, 3, Duration.ofMinutes(1));

    private final Bucket bucket = Bucket.builder()
        .addLimit(Bandwidth.builder()
            .capacity(properties.capacity())
            .refillGreedy(properties.refillTokens(), properties.refillPeriod())
            .build())
        .build();

    private final ProblemDetailWriter problemDetailWriter = new ProblemDetailWriter(new ObjectMapper());

    private final RateLimitFilter filter = new RateLimitFilter(bucket, problemDetailWriter, properties);

    private final WebFilterChain chain = exchange -> Mono.empty();

    @Test
    void allowsThreeRequestsThenRejectsFourth() {
        for (int i = 0; i < 3; i++) {
            MockServerWebExchange exchange = exchange();
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }

        MockServerWebExchange rejected = exchange();
        StepVerifier.create(filter.filter(rejected, chain)).verifyComplete();

        assertThat(rejected.getResponse().getStatusCode().value()).isEqualTo(429);
        assertThat(rejected.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotNull();

        String body = rejected.getResponse().getBodyAsString().block();
        assertThat(body).contains(
            "Rate limit of %d requests per %d seconds exceeded."
                .formatted(properties.capacity(), properties.refillPeriod().toSeconds()));
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/calculate").build());
    }
}
