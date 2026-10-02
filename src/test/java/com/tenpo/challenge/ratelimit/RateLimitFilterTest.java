package com.tenpo.challenge.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final Bucket bucket = Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofMinutes(1)).build())
        .build();

    private final ProblemDetailWriter problemDetailWriter = new ProblemDetailWriter(new ObjectMapper());

    private final RateLimitFilter filter = new RateLimitFilter(bucket, problemDetailWriter);

    private final WebFilterChain chain = exchange -> Mono.empty();

    @Test
    void allowsThreeRequestsThenRejectsFourth() {
        for (int i = 0; i < 3; i++) {
            ServerWebExchange exchange = exchange();
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }

        ServerWebExchange rejected = exchange();
        StepVerifier.create(filter.filter(rejected, chain)).verifyComplete();

        assertThat(rejected.getResponse().getStatusCode().value()).isEqualTo(429);
        assertThat(rejected.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotNull();
    }

    private ServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/calculate").build());
    }
}
