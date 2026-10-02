package com.tenpo.challenge.history.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryPageTest {

    @Test
    void hasNextIsTrueWhenMoreRowsThanSize() {
        PageQuery query = new PageQuery(0, 2);
        HistoryPage<String> page = HistoryPage.fromSlice(List.of("a", "b", "c"), query);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void hasNextIsFalseWhenRowsFitInSize() {
        PageQuery query = new PageQuery(1, 20);
        HistoryPage<String> page = HistoryPage.fromSlice(List.of("a", "b"), query);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.hasNext()).isFalse();
    }
}
