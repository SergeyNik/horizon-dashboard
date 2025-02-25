package com.github.sergeynik.horizondashboard.controller;

import com.github.sergeynik.horizondashboard.controller.mapper.TxMapper;
import com.github.sergeynik.horizondashboard.controller.mapper.dto.TxDto;
import com.github.sergeynik.horizondashboard.service.HorizonService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stream")
public class TransactionStreamController {

    private final HorizonService horizonService;
    private final TxMapper txMapper;

    @GetMapping(value = "/transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TxDto> transactions() {
        return horizonService.streamTransactions().map(txMapper::toTxDto);
    }

    @GetMapping(value = "/{account}/transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TxDto> transactionsAcc(@Pattern(regexp = "^G[ABCDEFGHIJKLMNOPQRSTUVWXYZ234567]{55}$") @PathVariable String account) {
        return horizonService.streamTransactionsByAccount(account).map(txMapper::toTxDto);
    }
}
