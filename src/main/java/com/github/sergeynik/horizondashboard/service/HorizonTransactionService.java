package com.github.sergeynik.horizondashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Optional;

@Slf4j
@Service
public class HorizonTransactionService {

    private final Server horizonServer;
    private final SSEStream<TransactionResponse> stream;
    private final Sinks.Many<TransactionResponse> sink;

    public HorizonTransactionService(Server horizonServer) {
        this.horizonServer = horizonServer;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.stream = horizonServer.transactions().cursor("now").stream(txStreamListener(sink));
    }

    private EventListener<TransactionResponse> txStreamListener(Sinks.Many<TransactionResponse> sink) {
        return new EventListener<>() {
            @Override
            public void onEvent(TransactionResponse transactionResponse) {
                log.info("Received transaction response: {}", transactionResponse);
                sink.tryEmitNext(transactionResponse);
            }

            @Override
            public void onFailure(Optional<Throwable> error, Optional<Integer> responseCode) {
                error.ifPresent(errorCode -> log.error("Error streaming transactions", errorCode));
            }
        };
    }

    public Flux<TransactionResponse> streamTransactions() {
        return sink.asFlux()
                .doOnSubscribe(subscription -> System.out.println("New subscriber connected!"))
                .doOnNext(transaction -> System.out.println("Emitting transaction: " + transaction.getId()))
                .doOnError(error -> System.err.println("Sink error: " + error.getMessage()));
    }

    public void stop() {
        stream.close();
        log.info("Stream Stopped!");
        horizonServer.close();
    }
}