package com.github.sergeynik.horizondashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.requests.TransactionsRequestBuilder;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class HorizonTransactionService {

    private enum LOCKS {LOCK_CURSOR_NOW, LOCK_CURSOR_NOW_WITH_ACCOUNT}

    private final AtomicInteger subscriberCount = new AtomicInteger();
    private final AtomicReference<Sinks.Many<TransactionResponse>> multicastSink = new AtomicReference<>();

    private static final String CURSOR_NOW = "now";
    private final Server horizonServer;
    private final ConcurrentMap<LOCKS, SSEStream<TransactionResponse>> horizonDataStreams;

    public HorizonTransactionService(Server horizonServer) {
        this.horizonServer = horizonServer;
        this.horizonDataStreams = new ConcurrentHashMap<>(LOCKS.values().length);
    }

    public Flux<TransactionResponse> streamTransactions() {
        horizonDataStreams.computeIfAbsent(LOCKS.LOCK_CURSOR_NOW, cursor -> {
            multicastSink.set(Sinks.many().multicast().onBackpressureBuffer());
            TransactionsRequestBuilder cursored = horizonServer.transactions().cursor(CURSOR_NOW);
            return cursored.stream(txStreamListener(multicastSink.get()));
        });
        return multicastSink.get().asFlux()
                .doOnSubscribe(subscription -> subscriberCount.incrementAndGet())
                .doOnCancel(() -> {
                    if (subscriberCount.decrementAndGet() == 0) {
                        stop(LOCKS.LOCK_CURSOR_NOW);
                        multicastSink.get().tryEmitComplete();
                    }
                })
                .doOnError(error -> {
                    stop(LOCKS.LOCK_CURSOR_NOW);
                    log.error("Error occurred while subscribing to transactions", error);
                });
    }

    public Flux<TransactionResponse> streamTransactionsByAccount(String account) {
        Sinks.Many<TransactionResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
        horizonDataStreams.computeIfAbsent(LOCKS.LOCK_CURSOR_NOW_WITH_ACCOUNT, cursor -> {
            TransactionsRequestBuilder cursored = horizonServer.transactions().forAccount(account).cursor(CURSOR_NOW);
            return cursored.stream(txStreamListener(sink));
        });
        return sink.asFlux()
                .doOnSubscribe(subscription -> log.info("Subscriber connected!"))
                .doOnCancel(() -> stop(LOCKS.LOCK_CURSOR_NOW_WITH_ACCOUNT));
    }

    private EventListener<TransactionResponse> txStreamListener(Sinks.Many<TransactionResponse> sink) {
        return new EventListener<>() {
            @Override
            public void onEvent(TransactionResponse transactionResponse) {
                log.info("Received transaction response: {}", transactionResponse.getSourceAccount());
                sink.tryEmitNext(transactionResponse);
            }

            @Override
            public void onFailure(Optional<Throwable> error, Optional<Integer> responseCode) {
                error.ifPresent(errorCode -> {
                    sink.tryEmitError(errorCode);
                    log.error("Error streaming transactions", errorCode);
                });
            }
        };
    }

    private void stop(LOCKS lockCursor) {
        horizonDataStreams.remove(lockCursor).close();
        log.info("Horizon SSE stream closed because no client");
    }

    @org.springframework.context.event.EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        for (SSEStream<TransactionResponse> stream : horizonDataStreams.values()) {
            stream.close();
        }
        horizonServer.close();
    }
}