package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import com.tenpo.challenge.history.application.port.in.RecordCallUseCase;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.capture.BoundedByteSink;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.capture.CachedBodyRequestDecorator;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.capture.CapturingResponseDecorator;
import com.tenpo.challenge.shared.web.ApiPaths;
import com.tenpo.challenge.shared.web.FilterOrder;
import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Order(FilterOrder.CALL_HISTORY)
public class CallHistoryFilter implements WebFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final byte[] EMPTY_BODY = new byte[0];

    private final RecordCallUseCase recordCallUseCase;
    private final CallRecordFactory callRecordFactory;
    private final ProblemDetailWriter problemDetailWriter;
    private final Scheduler historyScheduler;
    private final int maxRequestBytes;
    private final int maxResponseBytes;

    public CallHistoryFilter(
            RecordCallUseCase recordCallUseCase,
            CallRecordFactory callRecordFactory,
            ProblemDetailWriter problemDetailWriter,
            Scheduler historyScheduler,
            int maxRequestBytes,
            int maxResponseBytes) {
        this.recordCallUseCase = recordCallUseCase;
        this.callRecordFactory = callRecordFactory;
        this.problemDetailWriter = problemDetailWriter;
        this.historyScheduler = historyScheduler;
        this.maxRequestBytes = maxRequestBytes;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(exchange.getRequest().getPath().value())
            .filter(path -> PATH_MATCHER.match(ApiPaths.API_PATTERN, path))
            .map(path -> handle(exchange, chain))
            .defaultIfEmpty(Mono.defer(() -> chain.filter(exchange)))
            .flatMap(Function.identity());
    }

    private Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
        AtomicReference<byte[]> requestBytesHolder = new AtomicReference<>(EMPTY_BODY);
        BoundedByteSink responseSink = new BoundedByteSink(maxResponseBytes);
        CapturingResponseDecorator decoratedResponse =
            new CapturingResponseDecorator(exchange.getResponse(), responseSink);

        return DataBufferUtils.join(exchange.getRequest().getBody(), maxRequestBytes)
            .map(this::toBytes)
            .defaultIfEmpty(EMPTY_BODY)
            .doOnNext(requestBytesHolder::set)
            .flatMap(requestBytes -> continueChain(exchange, chain, decoratedResponse, requestBytes))
            .onErrorResume(DataBufferLimitException.class, error -> tooLarge(exchange, decoratedResponse))
            .doFinally(signal -> dispatch(exchange, requestBytesHolder.get(), decoratedResponse, responseSink));
    }

    private Mono<Void> continueChain(
            ServerWebExchange exchange, WebFilterChain chain, CapturingResponseDecorator decoratedResponse,
            byte[] requestBytes) {
        ServerWebExchange decoratedExchange = exchange.mutate()
            .request(new CachedBodyRequestDecorator(
                exchange.getRequest(), exchange.getResponse().bufferFactory(), requestBytes))
            .response(decoratedResponse)
            .build();
        return chain.filter(decoratedExchange);
    }

    private Mono<Void> tooLarge(ServerWebExchange exchange, CapturingResponseDecorator decoratedResponse) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Request body exceeds the maximum accepted size of " + maxRequestBytes + " bytes");
        problemDetail.setTitle("Payload Too Large");
        problemDetail.setInstance(exchange.getRequest().getURI());
        return problemDetailWriter.write(decoratedResponse, problemDetail);
    }

    private byte[] toBytes(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return bytes;
    }

    private void dispatch(
            ServerWebExchange exchange, byte[] requestBytes, CapturingResponseDecorator decoratedResponse,
            BoundedByteSink responseSink) {
        int status = Optional.ofNullable(decoratedResponse.getStatusCode())
            .map(HttpStatusCode::value)
            .orElse(HttpStatus.OK.value());
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        CallRecordFactory.CapturedBody requestBody =
            new CallRecordFactory.CapturedBody(new String(requestBytes, StandardCharsets.UTF_8), false);
        CallRecordFactory.CapturedBody responseBody =
            new CallRecordFactory.CapturedBody(responseSink.text(), responseSink.isTruncated());

        callRecordFactory.toCallRecord(
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().value(),
                queryParams,
                requestBody,
                status,
                responseBody)
            .flatMap(recordCallUseCase::record)
            .subscribeOn(historyScheduler)
            .onErrorResume(error -> Mono.empty())
            .subscribe();
    }
}
