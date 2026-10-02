package com.tenpo.challenge.calculation.infrastructure.adapter.in.web;

import com.tenpo.challenge.calculation.application.port.in.CalculateUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CalculationController {

    private final CalculateUseCase calculateUseCase;

    public CalculationController(CalculateUseCase calculateUseCase) {
        this.calculateUseCase = calculateUseCase;
    }

    @PostMapping("/api/v1/calculate")
    public Mono<CalculationResponse> calculate(@Valid @RequestBody CalculationRequest request) {
        return calculateUseCase.calculate(request.num1(), request.num2())
            .map(CalculationResponse::from);
    }
}
