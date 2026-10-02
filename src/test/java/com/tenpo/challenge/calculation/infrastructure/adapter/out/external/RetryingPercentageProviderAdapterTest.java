package com.tenpo.challenge.calculation.infrastructure.adapter.out.external;

import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.domain.Percentage;
import com.tenpo.challenge.shared.error.ExternalServiceUnavailableException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class RetryingPercentageProviderAdapterTest {

    @Test
    void retriesUpToThreeTimesOnUnavailable() {
        AtomicInteger attempts = new AtomicInteger();
        PercentageProviderPort delegate = () -> Mono.fromCallable(attempts::incrementAndGet)
            .flatMap(attempt -> Mono.<Percentage>error(new PercentageProviderUnavailableException("simulated")));

        RetryingPercentageProviderAdapter adapter = new RetryingPercentageProviderAdapter(
            delegate, buildRetry("unavailable-case"), Duration.ofMillis(500));

        StepVerifier.create(adapter.currentPercentage())
            .expectError(ExternalServiceUnavailableException.class)
            .verify();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryOnClientError() {
        AtomicInteger attempts = new AtomicInteger();
        PercentageProviderPort delegate = () -> Mono.fromCallable(attempts::incrementAndGet)
            .flatMap(attempt -> Mono.<Percentage>error(new PercentageProviderClientErrorException("simulated")));

        RetryingPercentageProviderAdapter adapter = new RetryingPercentageProviderAdapter(
            delegate, buildRetry("client-error-case"), Duration.ofMillis(500));

        StepVerifier.create(adapter.currentPercentage())
            .expectError(ExternalServiceUnavailableException.class)
            .verify();

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void succeedsOnSecondAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        Map<Integer, Supplier<Mono<Percentage>>> outcomesByAttempt = Map.of(
            1, () -> Mono.error(new PercentageProviderUnavailableException("simulated")),
            2, () -> Mono.just(Percentage.of(BigDecimal.TEN)));
        PercentageProviderPort delegate = () -> Mono.fromCallable(attempts::incrementAndGet)
            .flatMap(attempt -> outcomesByAttempt.get(attempt).get());

        RetryingPercentageProviderAdapter adapter = new RetryingPercentageProviderAdapter(
            delegate, buildRetry("success-second-case"), Duration.ofMillis(500));

        StepVerifier.create(adapter.currentPercentage())
            .expectNextCount(1)
            .verifyComplete();

        assertThat(attempts.get()).isEqualTo(2);
    }

    private Retry buildRetry(String name) {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ZERO)
            .retryExceptions(PercentageProviderUnavailableException.class, TimeoutException.class)
            .ignoreExceptions(PercentageProviderClientErrorException.class)
            .build();
        return Retry.of(name, retryConfig);
    }
}
