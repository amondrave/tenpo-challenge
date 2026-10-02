package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tenpo.challenge.history.domain.CallRecord;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static java.util.function.Predicate.not;

public class CallRecordFactory {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CallRecordFactory(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public record CapturedBody(String text, boolean truncated) {
    }

    public Mono<CallRecord> toCallRecord(
            String method,
            String path,
            MultiValueMap<String, String> queryParams,
            CapturedBody requestBody,
            int responseStatus,
            CapturedBody responseBody) {
        return Mono.zip(normalize(requestBody), normalize(responseBody))
            .flatMap(normalized -> Mono.fromCallable(
                () -> buildRecord(method, path, queryParams, normalized, responseStatus)));
    }

    private CallRecord buildRecord(
            String method,
            String path,
            MultiValueMap<String, String> queryParams,
            Tuple2<JsonNode, JsonNode> normalized,
            int responseStatus) throws JsonProcessingException {
        ObjectNode requestParams = objectMapper.createObjectNode();
        requestParams.set("query", objectMapper.valueToTree(queryParams.toSingleValueMap()));
        requestParams.set("body", normalized.getT1());
        return new CallRecord(
            null,
            Instant.now(clock),
            method,
            path,
            objectMapper.writeValueAsString(requestParams),
            responseStatus,
            objectMapper.writeValueAsString(normalized.getT2()));
    }

    private Mono<JsonNode> normalize(CapturedBody body) {
        return Mono.justOrEmpty(Optional.of(body.text()).filter(not(String::isBlank)))
            .filter(text -> !body.truncated())
            .flatMap(text -> Mono.fromCallable(() -> (JsonNode) objectMapper.readTree(text)))
            .onErrorResume(JsonProcessingException.class, error -> Mono.just(rawNode(body)))
            .switchIfEmpty(Mono.fromSupplier(() -> Optional.of(body.text())
                .filter(not(String::isBlank))
                .<JsonNode>map(text -> rawNode(body))
                .orElse(NullNode.getInstance())));
    }

    private JsonNode rawNode(CapturedBody body) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("truncated", body.truncated());
        node.put("raw", body.text());
        return node;
    }
}
