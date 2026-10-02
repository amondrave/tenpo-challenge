package com.tenpo.challenge.history.application.port.in;

import com.tenpo.challenge.history.domain.CallRecord;
import com.tenpo.challenge.history.domain.HistoryPage;
import com.tenpo.challenge.history.domain.PageQuery;
import reactor.core.publisher.Mono;

public interface QueryHistoryUseCase {

    Mono<HistoryPage<CallRecord>> find(PageQuery query);
}
