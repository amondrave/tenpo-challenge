package com.tenpo.challenge.shared.web;

import com.tenpo.challenge.shared.error.ExternalServiceUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
class ThrowingTestController {

    @PostMapping("/test/validate")
    public Mono<String> validate(@Valid @RequestBody TestRequest request) {
        return Mono.just("ok");
    }

    @PostMapping("/test/external")
    public Mono<String> external() {
        return Mono.error(new ExternalServiceUnavailableException("boom"));
    }

    @PostMapping("/test/not-found")
    public Mono<String> notFound() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "missing"));
    }

    @PostMapping("/test/unexpected")
    public Mono<String> unexpected() {
        return Mono.error(new IllegalStateException("internal secret: db-password=hunter2"));
    }

    record TestRequest(@NotNull BigDecimal num1) {
    }
}
