package com.tenpo.challenge.calculation.infrastructure.adapter.in.web;

import com.tenpo.challenge.calculation.application.port.in.CalculateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

    @Operation(summary = "Calculate num1 plus num2 plus the current external percentage")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Calculation succeeded"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "502", description = "Percentage provider unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/api/v1/calculate")
    public Mono<CalculationResponse> calculate(@Valid @RequestBody CalculationRequest request) {
        return calculateUseCase.calculate(request.num1(), request.num2())
            .map(CalculationResponse::from);
    }
}
