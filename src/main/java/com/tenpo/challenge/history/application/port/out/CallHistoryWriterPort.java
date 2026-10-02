package com.tenpo.challenge.history.application.port.out;

import com.tenpo.challenge.history.domain.CallRecord;
import reactor.core.publisher.Mono;

public interface CallHistoryWriterPort {

    Mono<Void> save(CallRecord callRecord);
}
