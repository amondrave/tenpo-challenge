package com.tenpo.challenge.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, ProblemDetail problemDetail) {
        return write(exchange.getResponse(), problemDetail);
    }

    public Mono<Void> write(ServerHttpResponse response, ProblemDetail problemDetail) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(problemDetail))
            .map(response.bufferFactory()::wrap)
            .flatMap(buffer -> writeTo(response, problemDetail, buffer));
    }

    private Mono<Void> writeTo(ServerHttpResponse response, ProblemDetail problemDetail, DataBuffer buffer) {
        response.setStatusCode(HttpStatus.valueOf(problemDetail.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return response.writeWith(Mono.just(buffer));
    }
}
