package com.github.sergeynik.horizondashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Sinks;

import java.util.Optional;

@Slf4j
@Component
public class HorizonListener {

    public EventListener<TransactionResponse> get(Sinks.Many<TransactionResponse> sink) {
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
}