package com.tenpo.challenge.history.application.service;

import com.tenpo.challenge.history.application.port.out.CallHistoryReaderPort;
import com.tenpo.challenge.history.application.port.out.CallHistoryWriterPort;
import com.tenpo.challenge.history.domain.CallRecord;
import com.tenpo.challenge.history.domain.PageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallHistoryServiceTest {

    @Mock
    private CallHistoryWriterPort writerPort;

    @Mock
    private CallHistoryReaderPort readerPort;

    @Test
    void recordDelegatesToWriterPort() {
        CallRecord record = new CallRecord(null, Instant.now(), "GET", "/api/v1/history", "{}", 200, "{}");
        when(writerPort.save(record)).thenReturn(Mono.empty());

        CallHistoryService service = new CallHistoryService(writerPort, readerPort);

        StepVerifier.create(service.record(record)).verifyComplete();
    }

    @Test
    void writeFailureIsLoggedAndSwallowed() {
        CallRecord record = new CallRecord(null, Instant.now(), "GET", "/api/v1/history", "{}", 200, "{}");
        when(writerPort.save(record)).thenReturn(Mono.error(new RuntimeException("db down")));

        CallHistoryService service = new CallHistoryService(writerPort, readerPort);

        StepVerifier.create(service.record(record)).verifyComplete();
    }

    @Test
    void findDelegatesToReaderPortAndBuildsPage() {
        CallRecord a = new CallRecord(2L, Instant.now(), "GET", "/api/v1/history", "{}", 200, "{}");
        CallRecord b = new CallRecord(1L, Instant.now(), "POST", "/api/v1/calculate", "{}", 200, "{}");
        when(readerPort.findSlice(0L, 2)).thenReturn(Flux.just(a, b));

        CallHistoryService service = new CallHistoryService(writerPort, readerPort);

        StepVerifier.create(service.find(new PageQuery(0, 1)))
            .assertNext(page -> {
                assertThat(page.content()).containsExactly(a);
                assertThat(page.hasNext()).isTrue();
            })
            .verifyComplete();
    }
}
