package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

class CachedBodyRequestDecoratorTest {

    @Test
    void replaysBodyOnEverySubscription() {
        ServerHttpRequest delegate = MockServerHttpRequest.post("/api/v1/calculate").build();
        CachedBodyRequestDecorator decorator = new CachedBodyRequestDecorator(
            delegate, new DefaultDataBufferFactory(), "{\"a\":1}".getBytes(StandardCharsets.UTF_8));

        StepVerifier.create(decorator.getBody().map(this::toText))
            .expectNext("{\"a\":1}")
            .verifyComplete();
        StepVerifier.create(decorator.getBody().map(this::toText))
            .expectNext("{\"a\":1}")
            .verifyComplete();
    }

    private String toText(org.springframework.core.io.buffer.DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
