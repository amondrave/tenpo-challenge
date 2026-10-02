package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

public class CapturingResponseDecorator extends ServerHttpResponseDecorator {

    private final BoundedByteSink sink;

    public CapturingResponseDecorator(ServerHttpResponse delegate, BoundedByteSink sink) {
        super(delegate);
        this.sink = sink;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return super.writeWith(Flux.from(body).doOnNext(this::capture));
    }

    private void capture(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.toByteBuffer(0, ByteBuffer.wrap(bytes), 0, bytes.length);
        sink.write(bytes);
    }
}
