package com.github.sergeynik.horizondashboard.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class HorizonTransactionService {

    private static final String CURSOR_NOW = "now";
    private final Server horizonServer;
    private final ConcurrentMap<String, SSEStream<TransactionResponse>> singleStream;

    public HorizonTransactionService(Server horizonServer) {
        this.horizonServer = horizonServer;
        this.singleStream = new ConcurrentHashMap<>(1);
    }

    public Flux<TransactionResponse> streamTransactions() {
        Sinks.Many<TransactionResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
        singleStream.computeIfAbsent(CURSOR_NOW, (cursor) -> {
            TransactionsRequestBuilder cursored = horizonServer.transactions().cursor(CURSOR_NOW);
            return cursored.stream(txStreamListener(sink));
        });
        return sink.asFlux()
                .doOnSubscribe(subscription -> log.info("Subscriber connected!"))
                .doOnCancel(() -> {
                    stop();
                    log.info("SSE stream is closed because the client has closed the connection");
                });
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
                error.ifPresent(errorCode -> log.error("Error streaming transactions", errorCode));
            }
        };
    }

    private void stop() {
        if (!singleStream.isEmpty()) {
            singleStream.get(CURSOR_NOW).close();
            singleStream.clear();
            log.info("Stream Stopped!");
        }
    }
}