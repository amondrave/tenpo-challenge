package com.tenpo.challenge.calculation.application.port.in;

import com.tenpo.challenge.calculation.domain.Calculation;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CalculateUseCase {

    Mono<Calculation> calculate(BigDecimal num1, BigDecimal num2);
}
