package com.tenpo.challenge.history.infrastructure.adapter.out.persistence;

import com.tenpo.challenge.history.domain.CallRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class R2dbcCallHistoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DatabaseClient databaseClient;

    private R2dbcCallHistoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new R2dbcCallHistoryAdapter(databaseClient);
    }

    @Test
    void savesAndReadsBackInDescendingOrder() {
        CallRecord older = new CallRecord(
            null, Instant.now().minusSeconds(10), "GET", "/api/v1/history-test-a", "{\"query\":{}}", 200, "{}");
        CallRecord newer = new CallRecord(
            null, Instant.now(), "POST", "/api/v1/calculate-test-a", "{\"query\":{}}", 200, "{\"result\":11}");

        StepVerifier.create(adapter.save(older).then(adapter.save(newer))).verifyComplete();

        StepVerifier.create(adapter.findSlice(0, 2).collectList())
            .assertNext(rows -> {
                assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
                assertThat(rows.get(0).occurredAt()).isAfterOrEqualTo(rows.get(1).occurredAt());
            })
            .verifyComplete();
    }

    @Test
    void limitBoundsReturnedRowCount() {
        StepVerifier.create(
                adapter.save(new CallRecord(null, Instant.now(), "GET", "/x1", "{}", 200, "{}"))
                    .then(adapter.save(new CallRecord(null, Instant.now(), "GET", "/x2", "{}", 200, "{}")))
                    .then(adapter.save(new CallRecord(null, Instant.now(), "GET", "/x3", "{}", 200, "{}"))))
            .verifyComplete();

        StepVerifier.create(adapter.findSlice(0, 1).collectList())
            .assertNext(rows -> assertThat(rows).hasSize(1))
            .verifyComplete();
    }
}
