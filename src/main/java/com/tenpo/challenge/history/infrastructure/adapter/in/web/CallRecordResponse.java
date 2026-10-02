package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.tenpo.challenge.history.domain.CallRecord;

import java.time.Instant;

public record CallRecordResponse(
    Long id,
    Instant occurredAt,
    String method,
    String path,
    @JsonRawValue String requestParams,
    int responseStatus,
    @JsonRawValue String responseBody) {

    public static CallRecordResponse from(CallRecord callRecord) {
        return new CallRecordResponse(
            callRecord.id(),
            callRecord.occurredAt(),
            callRecord.method(),
            callRecord.path(),
            callRecord.requestParamsJson(),
            callRecord.responseStatus(),
            callRecord.responseBodyJson());
    }
}
