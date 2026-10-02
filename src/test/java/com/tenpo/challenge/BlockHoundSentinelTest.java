package com.tenpo.challenge;

import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class BlockHoundSentinelTest {

    @Test
    void detectsBlockingCallOnParallelScheduler() {
        Mono<Integer> blockingMono = Mono.fromCallable(() -> {
            Thread.sleep(1);
            return 1;
        }).subscribeOn(Schedulers.parallel());

        StepVerifier.create(blockingMono)
            .expectErrorMatches(BlockingOperationError.class::isInstance)
            .verify();
    }
}
