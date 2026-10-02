package com.tenpo.challenge.calculation.application.service;

import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.domain.Percentage;
import com.tenpo.challenge.shared.error.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock
    private PercentageProviderPort percentageProviderPort;

    @Test
    void calculatesResultUsingProvidedPercentage() {
        when(percentageProviderPort.currentPercentage())
            .thenReturn(Mono.just(new Percentage(BigDecimal.TEN)));

        CalculationService service = new CalculationService(percentageProviderPort);

        StepVerifier.create(service.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(calculation -> assertThat(calculation.result()).isEqualByComparingTo(BigDecimal.valueOf(11)))
            .verifyComplete();
    }

    @Test
    void propagatesProviderError() {
        when(percentageProviderPort.currentPercentage())
            .thenReturn(Mono.error(new ExternalServiceUnavailableException("down")));

        CalculationService service = new CalculationService(percentageProviderPort);

        StepVerifier.create(service.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .expectError(ExternalServiceUnavailableException.class)
            .verify();
    }

    @Test
    void doesNotCachePercentageAcrossSequentialCalls() {
        when(percentageProviderPort.currentPercentage())
            .thenReturn(Mono.just(new Percentage(BigDecimal.TEN)));

        CalculationService service = new CalculationService(percentageProviderPort);

        StepVerifier.create(service.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .expectNextCount(1)
            .verifyComplete();
        StepVerifier.create(service.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .expectNextCount(1)
            .verifyComplete();

        verify(percentageProviderPort, times(2)).currentPercentage();
    }
}
