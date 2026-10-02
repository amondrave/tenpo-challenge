package com.tenpo.challenge.calculation.infrastructure.adapter.out.external;

import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.domain.Percentage;
import com.tenpo.challenge.shared.error.ExternalServiceUnavailableException;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class RetryingPercentageProviderAdapter implements PercentageProviderPort {

    private final PercentageProviderPort delegate;
    private final Retry retry;
    private final Duration timeout;

    public RetryingPercentageProviderAdapter(PercentageProviderPort delegate, Retry retry, Duration timeout) {
        this.delegate = delegate;
        this.retry = retry;
        this.timeout = timeout;
    }

    @Override
    public Mono<Percentage> currentPercentage() {
        return Mono.defer(delegate::currentPercentage)
            .timeout(timeout)
            .transformDeferred(RetryOperator.of(retry))
            .onErrorMap(this::toExternalServiceUnavailable);
    }

    private ExternalServiceUnavailableException toExternalServiceUnavailable(Throwable throwable) {
        return new ExternalServiceUnavailableException("Percentage provider unavailable", throwable);
    }
}
