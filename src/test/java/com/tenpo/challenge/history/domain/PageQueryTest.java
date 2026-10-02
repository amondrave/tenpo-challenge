package com.tenpo.challenge.history.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    @Test
    void offsetIsPageTimesSize() {
        assertThat(new PageQuery(0, 20).offset()).isZero();
        assertThat(new PageQuery(1, 20).offset()).isEqualTo(20L);
        assertThat(new PageQuery(2, 15).offset()).isEqualTo(30L);
    }
}
