package com.github.sergeynik.horizondashboard.controller;

import com.github.sergeynik.horizondashboard.service.HorizonTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class TransactionStreamController {

    private final HorizonTransactionService horizonTransactionService;

    @GetMapping(value = "/stream-transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> transactions() {
        return horizonTransactionService.streamTransactions().map(TransactionResponse::getSourceAccount);
    }
}
