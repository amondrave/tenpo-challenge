package com.tenpo.challenge.history.domain;

import java.time.Instant;

public record CallRecord(
    Long id,
    Instant occurredAt,
    String method,
    String path,
    String requestParamsJson,
    int responseStatus,
    String responseBodyJson) {
}
