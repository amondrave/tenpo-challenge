package com.tenpo.challenge.history.application.port.in;

import com.tenpo.challenge.history.domain.CallRecord;
import reactor.core.publisher.Mono;

public interface RecordCallUseCase {

    Mono<Void> record(CallRecord callRecord);
}
