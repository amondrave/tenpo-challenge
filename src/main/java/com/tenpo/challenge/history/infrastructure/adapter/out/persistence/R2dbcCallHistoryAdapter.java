package com.tenpo.challenge.history.infrastructure.adapter.out.persistence;

import com.tenpo.challenge.history.application.port.out.CallHistoryReaderPort;
import com.tenpo.challenge.history.application.port.out.CallHistoryWriterPort;
import com.tenpo.challenge.history.domain.CallRecord;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class R2dbcCallHistoryAdapter implements CallHistoryWriterPort, CallHistoryReaderPort {

    private static final String INSERT_SQL = """
        INSERT INTO call_history (occurred_at, method, path, request_params, response_status, response_body)
        VALUES (:occurredAt, :method, :path, :requestParams, :responseStatus, :responseBody)
        """;

    private static final String SELECT_SLICE_SQL = """
        SELECT id, occurred_at, method, path, request_params, response_status, response_body
        FROM call_history
        ORDER BY occurred_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """;

    private final DatabaseClient databaseClient;

    public R2dbcCallHistoryAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> save(CallRecord callRecord) {
        return databaseClient.sql(INSERT_SQL)
            .bind("occurredAt", callRecord.occurredAt())
            .bind("method", callRecord.method())
            .bind("path", callRecord.path())
            .bind("requestParams", Json.of(callRecord.requestParamsJson()))
            .bind("responseStatus", callRecord.responseStatus())
            .bind("responseBody", Json.of(callRecord.responseBodyJson()))
            .then();
    }

    @Override
    public Flux<CallRecord> findSlice(long offset, int limit) {
        return databaseClient.sql(SELECT_SLICE_SQL)
            .bind("limit", limit)
            .bind("offset", offset)
            .map(this::toCallRecord)
            .all();
    }

    private CallRecord toCallRecord(Row row, RowMetadata metadata) {
        return new CallRecord(
            row.get("id", Long.class),
            row.get("occurred_at", Instant.class),
            row.get("method", String.class),
            row.get("path", String.class),
            row.get("request_params", Json.class).asString(),
            row.get("response_status", Integer.class),
            row.get("response_body", Json.class).asString());
    }
}
