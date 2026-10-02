package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CapturingResponseDecoratorTest {

    @Test
    void capturesWrittenBytesWithoutConsumingBuffer() {
        MockServerHttpResponse delegate = new MockServerHttpResponse();
        BoundedByteSink sink = new BoundedByteSink(1024);
        CapturingResponseDecorator decorator = new CapturingResponseDecorator(delegate, sink);

        DataBuffer buffer = new DefaultDataBufferFactory().wrap("hello".getBytes(StandardCharsets.UTF_8));

        StepVerifier.create(decorator.writeWith(Mono.just(buffer))).verifyComplete();

        assertThat(sink.text()).isEqualTo("hello");
        assertThat(delegate.getBodyAsString().block()).isEqualTo("hello");
    }

    @Test
    void truncatesCapturedBodyBeyondLimit() {
        MockServerHttpResponse delegate = new MockServerHttpResponse();
        BoundedByteSink sink = new BoundedByteSink(3);
        CapturingResponseDecorator decorator = new CapturingResponseDecorator(delegate, sink);

        DataBuffer buffer = new DefaultDataBufferFactory().wrap("hello".getBytes(StandardCharsets.UTF_8));

        StepVerifier.create(decorator.writeWith(Mono.just(buffer))).verifyComplete();

        assertThat(sink.isTruncated()).isTrue();
        assertThat(delegate.getBodyAsString().block()).isEqualTo("hello");
    }
}
