package com.tenpo.challenge.calculation.application.port.out;

import com.tenpo.challenge.calculation.domain.Percentage;
import reactor.core.publisher.Mono;

public interface PercentageProviderPort {

    Mono<Percentage> currentPercentage();
}
