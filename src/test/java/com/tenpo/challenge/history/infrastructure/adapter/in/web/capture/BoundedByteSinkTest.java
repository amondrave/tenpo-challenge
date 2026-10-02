package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedByteSinkTest {

    @Test
    void writesWithinCapAreNotTruncated() {
        BoundedByteSink sink = new BoundedByteSink(10);
        sink.write("hello".getBytes(StandardCharsets.UTF_8));

        assertThat(sink.text()).isEqualTo("hello");
        assertThat(sink.isTruncated()).isFalse();
    }

    @Test
    void writesExceedingCapAreTruncated() {
        BoundedByteSink sink = new BoundedByteSink(5);
        sink.write("hello world".getBytes(StandardCharsets.UTF_8));

        assertThat(sink.text()).isEqualTo("hello");
        assertThat(sink.isTruncated()).isTrue();
    }
}
