package com.github.sergeynik.horizondashboard.controller;

import com.github.sergeynik.horizondashboard.controller.mapper.TxMapper;
import com.github.sergeynik.horizondashboard.controller.mapper.dto.TxDto;
import com.github.sergeynik.horizondashboard.service.HorizonTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class TransactionStreamController {

    private final HorizonTransactionService horizonTransactionService;
    private final TxMapper txMapper;

    @GetMapping(value = "/stream-transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TxDto> transactions() {
        return horizonTransactionService.streamTransactions().map(txMapper::toTxDto);
    }
}
