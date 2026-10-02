package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.tenpo.challenge.history.application.port.in.QueryHistoryUseCase;
import com.tenpo.challenge.history.domain.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HistoryController {

    private final QueryHistoryUseCase queryHistoryUseCase;

    public HistoryController(QueryHistoryUseCase queryHistoryUseCase) {
        this.queryHistoryUseCase = queryHistoryUseCase;
    }

    @GetMapping("/api/v1/history")
    public Mono<HistoryPageResponse> history(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return queryHistoryUseCase.find(new PageQuery(page, size))
            .map(HistoryPageResponse::from);
    }
}
