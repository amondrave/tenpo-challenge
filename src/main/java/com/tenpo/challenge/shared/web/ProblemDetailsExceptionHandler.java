package com.tenpo.challenge.shared.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tenpo.challenge.shared.error.ExternalServiceUnavailableException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@RestControllerAdvice
public class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI EXTERNAL_SERVICE_UNAVAILABLE_TYPE =
        URI.create("urn:tenpo:problem:external-service-unavailable");

    private static final Map<HttpStatusCode, String> STATUS_TITLES = Map.of(
        HttpStatus.NOT_FOUND, "Resource Not Found",
        HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");

    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
            WebExchangeBindException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        return Mono.just(validationResponse(status, headers, exchange, fieldErrors(ex.getFieldErrors())));
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleServerWebInputException(
            ServerWebInputException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        return Mono.just(validationResponse(status, headers, exchange, List.of(fieldViolation(ex))));
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        return Mono.just(validationResponse(status, headers, exchange, parameterErrors(ex)));
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleResponseStatusException(
            ResponseStatusException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        return Mono.just(statusResponse(status, headers, exchange, Optional.ofNullable(ex.getReason())
            .orElse("The requested resource was not found")));
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleMethodNotAllowedException(
            MethodNotAllowedException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
        return Mono.just(statusResponse(status, headers, exchange, Optional.ofNullable(ex.getReason())
            .orElse("The HTTP method is not supported for this resource")));
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleExternalServiceUnavailable(
            ExternalServiceUnavailableException ex, ServerWebExchange exchange) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY, "The upstream percentage provider is unavailable");
        problemDetail.setTitle("External Service Unavailable");
        problemDetail.setType(EXTERNAL_SERVICE_UNAVAILABLE_TYPE);
        problemDetail.setInstance(exchange.getRequest().getURI());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problemDetail));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(exchange.getRequest().getURI());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail));
    }

    private ResponseEntity<Object> validationResponse(
            HttpStatusCode status, HttpHeaders headers, ServerWebExchange exchange, List<Map<String, String>> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problemDetail.setTitle("Validation Failed");
        problemDetail.setInstance(exchange.getRequest().getURI());
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(status).headers(headers).body(problemDetail);
    }

    private ResponseEntity<Object> statusResponse(
            HttpStatusCode status, HttpHeaders headers, ServerWebExchange exchange, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(STATUS_TITLES.getOrDefault(status, status.toString()));
        problemDetail.setInstance(exchange.getRequest().getURI());
        return ResponseEntity.status(status).headers(headers).body(problemDetail);
    }

    private List<Map<String, String>> fieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
            .map(fieldError -> Map.of(
                "field", fieldError.getField(),
                "message", Optional.ofNullable(fieldError.getDefaultMessage()).orElse("is invalid")))
            .toList();
    }

    private Map<String, String> fieldViolation(ServerWebInputException ex) {
        return Stream.iterate(ex.getCause(), Objects::nonNull, Throwable::getCause)
            .filter(InvalidFormatException.class::isInstance)
            .map(InvalidFormatException.class::cast)
            .findFirst()
            .map(this::toFieldViolation)
            .orElseGet(() -> Map.of(
                "field", "body",
                "message", Optional.ofNullable(ex.getReason()).orElse("Malformed request body")));
    }

    private List<Map<String, String>> parameterErrors(HandlerMethodValidationException ex) {
        return ex.getParameterValidationResults().stream()
            .map(result -> Map.of(
                "field", Optional.ofNullable(result.getMethodParameter().getParameterName()).orElse("parameter"),
                "message", result.getResolvableErrors().stream()
                    .findFirst()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .orElse("is invalid")))
            .toList();
    }

    private Map<String, String> toFieldViolation(InvalidFormatException invalidFormatException) {
        return Map.of(
            "field", invalidFormatException.getPath().stream()
                .reduce((first, second) -> second)
                .map(JsonMappingException.Reference::getFieldName)
                .orElse("body"),
            "message", "must be numeric");
    }
}
