package com.tenpo.challenge.history.application.service;

import com.tenpo.challenge.history.application.port.in.QueryHistoryUseCase;
import com.tenpo.challenge.history.application.port.in.RecordCallUseCase;
import com.tenpo.challenge.history.application.port.out.CallHistoryReaderPort;
import com.tenpo.challenge.history.application.port.out.CallHistoryWriterPort;
import com.tenpo.challenge.history.domain.CallRecord;
import com.tenpo.challenge.history.domain.HistoryPage;
import com.tenpo.challenge.history.domain.PageQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class CallHistoryService implements RecordCallUseCase, QueryHistoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CallHistoryService.class);

    private final CallHistoryWriterPort writerPort;
    private final CallHistoryReaderPort readerPort;

    public CallHistoryService(CallHistoryWriterPort writerPort, CallHistoryReaderPort readerPort) {
        this.writerPort = writerPort;
        this.readerPort = readerPort;
    }

    @Override
    public Mono<Void> record(CallRecord callRecord) {
        return writerPort.save(callRecord)
            .onErrorResume(error -> Mono.fromRunnable(() -> log.warn("Failed to record call history entry", error)));
    }

    @Override
    public Mono<HistoryPage<CallRecord>> find(PageQuery query) {
        return readerPort.findSlice(query.offset(), query.size() + 1)
            .collectList()
            .map(rows -> HistoryPage.fromSlice(rows, query));
    }
}
