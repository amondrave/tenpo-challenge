package com.tenpo.challenge.calculation.infrastructure.adapter.out.external;

import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.domain.Percentage;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Supplier;

public class MockPercentageAdapter implements PercentageProviderPort {

    private final PercentageProviderProperties properties;
    private final Map<FailureMode, Supplier<Mono<Percentage>>> behaviors;

    public MockPercentageAdapter(PercentageProviderProperties properties) {
        this.properties = properties;
        this.behaviors = Map.of(
            FailureMode.NONE, () -> Mono.just(Percentage.of(properties.value())),
            FailureMode.UNAVAILABLE, () -> Mono.error(new PercentageProviderUnavailableException("simulated 503")),
            FailureMode.CLIENT_ERROR, () -> Mono.error(new PercentageProviderClientErrorException("simulated 400")));
    }

    @Override
    public Mono<Percentage> currentPercentage() {
        return Mono.defer(() -> behaviors.get(properties.failureMode()).get());
    }
}
