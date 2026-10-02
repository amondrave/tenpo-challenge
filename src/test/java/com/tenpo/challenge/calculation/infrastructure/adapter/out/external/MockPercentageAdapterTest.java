package com.tenpo.challenge.calculation.infrastructure.adapter.out.external;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MockPercentageAdapterTest {

    @Test
    void noneFailureModeReturnsConfiguredPercentage() {
        MockPercentageAdapter adapter = new MockPercentageAdapter(
            new PercentageProviderProperties(BigDecimal.TEN, FailureMode.NONE, Duration.ofMillis(500)));

        StepVerifier.create(adapter.currentPercentage())
            .assertNext(percentage -> assertThat(percentage.value()).isEqualByComparingTo(BigDecimal.TEN))
            .verifyComplete();
    }

    @Test
    void unavailableFailureModeEmitsUnavailableException() {
        MockPercentageAdapter adapter = new MockPercentageAdapter(
            new PercentageProviderProperties(BigDecimal.TEN, FailureMode.UNAVAILABLE, Duration.ofMillis(500)));

        StepVerifier.create(adapter.currentPercentage())
            .expectError(PercentageProviderUnavailableException.class)
            .verify();
    }

    @Test
    void clientErrorFailureModeEmitsClientErrorException() {
        MockPercentageAdapter adapter = new MockPercentageAdapter(
            new PercentageProviderProperties(BigDecimal.TEN, FailureMode.CLIENT_ERROR, Duration.ofMillis(500)));

        StepVerifier.create(adapter.currentPercentage())
            .expectError(PercentageProviderClientErrorException.class)
            .verify();
    }
}
