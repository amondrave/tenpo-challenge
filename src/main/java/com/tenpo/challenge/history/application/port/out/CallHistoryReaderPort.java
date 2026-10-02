package com.tenpo.challenge.history.application.port.out;

import com.tenpo.challenge.history.domain.CallRecord;
import reactor.core.publisher.Flux;

public interface CallHistoryReaderPort {

    Flux<CallRecord> findSlice(long offset, int limit);
}
