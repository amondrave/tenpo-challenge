package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

public class CachedBodyRequestDecorator extends ServerHttpRequestDecorator {

    private final DataBufferFactory bufferFactory;
    private final byte[] body;

    public CachedBodyRequestDecorator(ServerHttpRequest delegate, DataBufferFactory bufferFactory, byte[] body) {
        super(delegate);
        this.bufferFactory = bufferFactory;
        this.body = body;
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return Flux.defer(() -> Flux.just(bufferFactory.wrap(body)));
    }
}
