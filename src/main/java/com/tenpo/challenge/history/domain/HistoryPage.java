package com.tenpo.challenge.history.domain;

import java.util.List;

public record HistoryPage<T>(List<T> content, int page, int size, boolean hasNext) {

    public static <T> HistoryPage<T> fromSlice(List<T> rows, PageQuery query) {
        return new HistoryPage<>(
            rows.stream().limit(query.size()).toList(),
            query.page(),
            query.size(),
            rows.size() > query.size());
    }
}
