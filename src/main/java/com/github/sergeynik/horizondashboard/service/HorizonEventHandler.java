package com.github.sergeynik.horizondashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.requests.TransactionsRequestBuilder;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class HorizonEventHandler {

    private final HorizonListener horizonListener;

    public Flux<TransactionResponse> start(TransactionsRequestBuilder server, Runnable cleanupState) {
        Sinks.Many<TransactionResponse> sink = Sinks.many().multicast().onBackpressureBuffer();
        SSEStream<TransactionResponse> sseStream = server.stream(horizonListener.get(sink));
        AtomicInteger subscriberCount = new AtomicInteger();
        return sink.asFlux()
                .doOnSubscribe(subscription -> subscriberCount.incrementAndGet())
                .doOnCancel(() -> {
                    if (subscriberCount.decrementAndGet() == 0) {
                        sseStream.close();
                        sink.tryEmitComplete();
                        cleanupState.run();
                    }
                })
                .doOnError(error -> {
                    sseStream.close();
                    cleanupState.run();
                    log.error("Error occurred while subscribing to transactions", error);
                });
    }

    public Flux<TransactionResponse> startForAccount(TransactionsRequestBuilder server, Runnable cleanupState) {
        Sinks.Many<TransactionResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
        SSEStream<TransactionResponse> sseStream = server.stream(horizonListener.get(sink));
        return sink.asFlux()
                .doOnCancel(() -> {
                    sseStream.close();
                    sink.tryEmitComplete();
                    cleanupState.run();
                })
                .doOnError(error -> {
                    sseStream.close();
                    cleanupState.run();
                    log.error("Error occurred while subscribing to transactions", error);
                });
    }
}
