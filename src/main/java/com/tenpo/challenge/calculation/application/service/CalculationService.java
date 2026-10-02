package com.tenpo.challenge.calculation.application.service;

import com.tenpo.challenge.calculation.application.port.in.CalculateUseCase;
import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.domain.Calculation;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public class CalculationService implements CalculateUseCase {

    private final PercentageProviderPort percentageProviderPort;

    public CalculationService(PercentageProviderPort percentageProviderPort) {
        this.percentageProviderPort = percentageProviderPort;
    }

    @Override
    public Mono<Calculation> calculate(BigDecimal num1, BigDecimal num2) {
        return percentageProviderPort.currentPercentage()
            .map(percentage -> Calculation.of(num1, num2, percentage));
    }
}
