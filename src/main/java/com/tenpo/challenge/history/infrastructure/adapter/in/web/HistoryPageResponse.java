package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.tenpo.challenge.history.domain.CallRecord;
import com.tenpo.challenge.history.domain.HistoryPage;

import java.util.List;

public record HistoryPageResponse(List<CallRecordResponse> content, int page, int size, boolean hasNext) {

    public static HistoryPageResponse from(HistoryPage<CallRecord> page) {
        return new HistoryPageResponse(
            page.content().stream().map(CallRecordResponse::from).toList(),
            page.page(),
            page.size(),
            page.hasNext());
    }
}
