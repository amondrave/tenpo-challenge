package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CallRecordFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final CallRecordFactory factory = new CallRecordFactory(objectMapper, clock);

    @Test
    void validJsonBodyIsStoredAsIs() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        CallRecordFactory.CapturedBody requestBody = new CallRecordFactory.CapturedBody("{\"num1\":5}", false);
        CallRecordFactory.CapturedBody responseBody = new CallRecordFactory.CapturedBody("{\"result\":11}", false);

        StepVerifier.create(factory.toCallRecord("POST", "/api/v1/calculate", query, requestBody, 200, responseBody))
            .assertNext(record -> {
                assertThat(record.requestParamsJson()).contains("\"num1\":5");
                assertThat(record.responseBodyJson()).isEqualTo("{\"result\":11}");
                assertThat(record.responseStatus()).isEqualTo(200);
                assertThat(record.occurredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
            })
            .verifyComplete();
    }

    @Test
    void invalidJsonBodyBecomesRawNode() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        CallRecordFactory.CapturedBody requestBody = new CallRecordFactory.CapturedBody("not-json", false);
        CallRecordFactory.CapturedBody responseBody = new CallRecordFactory.CapturedBody("", false);

        StepVerifier.create(factory.toCallRecord("POST", "/api/v1/calculate", query, requestBody, 400, responseBody))
            .assertNext(record -> {
                assertThat(record.requestParamsJson()).contains("\"raw\":\"not-json\"");
                assertThat(record.responseBodyJson()).isEqualTo("null");
            })
            .verifyComplete();
    }

    @Test
    void truncatedBodyBecomesRawNodeEvenIfValidJson() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        CallRecordFactory.CapturedBody requestBody = new CallRecordFactory.CapturedBody("{\"a\":1}", true);
        CallRecordFactory.CapturedBody responseBody = new CallRecordFactory.CapturedBody("", false);

        StepVerifier.create(factory.toCallRecord("POST", "/api/v1/calculate", query, requestBody, 200, responseBody))
            .assertNext(record -> assertThat(record.requestParamsJson()).contains("\"truncated\":true"))
            .verifyComplete();
    }
}
