package com.tenpo.challenge.ratelimit;

import com.tenpo.challenge.shared.web.ApiPaths;
import com.tenpo.challenge.shared.web.FilterOrder;
import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@Order(FilterOrder.RATE_LIMIT)
public class RateLimitFilter implements WebFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final Bucket bucket;
    private final ProblemDetailWriter problemDetailWriter;
    private final RateLimitProperties properties;

    public RateLimitFilter(Bucket bucket, ProblemDetailWriter problemDetailWriter, RateLimitProperties properties) {
        this.bucket = bucket;
        this.problemDetailWriter = problemDetailWriter;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(exchange.getRequest().getPath().value())
            .filter(path -> PATH_MATCHER.match(ApiPaths.API_PATTERN, path))
            .map(path -> consumeAndProceed(exchange, chain))
            .defaultIfEmpty(Mono.defer(() -> chain.filter(exchange)))
            .flatMap(Function.identity());
    }

    private Mono<Void> consumeAndProceed(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.fromSupplier(() -> bucket.tryConsumeAndReturnRemaining(1))
            .flatMap(probe -> Mono.just(probe)
                .filter(ConsumptionProbe::isConsumed)
                .map(consumed -> chain.filter(exchange))
                .defaultIfEmpty(Mono.defer(() -> reject(exchange, probe)))
                .flatMap(Function.identity()));
    }

    private Mono<Void> reject(ServerWebExchange exchange, ConsumptionProbe probe) {
        long waitSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
        exchange.getResponse().getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(waitSeconds));
        return problemDetailWriter.write(exchange, tooManyRequestsProblem(exchange, waitSeconds));
    }

    private ProblemDetail tooManyRequestsProblem(ServerWebExchange exchange, long waitSeconds) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Rate limit of %d requests per %d seconds exceeded. Retry after %d seconds."
                .formatted(properties.capacity(), properties.refillPeriod().toSeconds(), waitSeconds));
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setInstance(exchange.getRequest().getURI());
        return problemDetail;
    }
}
