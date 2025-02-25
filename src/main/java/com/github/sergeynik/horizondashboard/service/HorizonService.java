package com.github.sergeynik.horizondashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.TransactionResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class HorizonService implements DisposableBean {

    private static final String CURSOR_NOW = "now";

    private final Server horizonServer;
    private final HorizonEventHandler horizonEventHandler;
    private final ConcurrentMap<Locks, Flux<TransactionResponse>> horizonSseStreams;

    public HorizonService(HorizonEventHandler horizonEventHandler, Server horizonServer) {
        this.horizonServer = horizonServer;
        this.horizonEventHandler = horizonEventHandler;
        this.horizonSseStreams = new ConcurrentHashMap<>(Locks.values().length);
    }

    public Flux<TransactionResponse> streamTransactions() {
        return horizonSseStreams.computeIfAbsent(Locks.LOCK_CURSOR_NOW,
                lock -> horizonEventHandler.start(horizonServer.transactions().cursor(CURSOR_NOW), () -> dropFlux(lock)));
    }

    public Flux<TransactionResponse> streamTransactionsByAccount(String account) {
        return horizonSseStreams.computeIfAbsent(Locks.LOCK_CURSOR_NOW_WITH_ACCOUNT,
                lock -> horizonEventHandler.startForAccount(horizonServer.transactions().forAccount(account).cursor(CURSOR_NOW), () -> dropFlux(lock)));
    }

    private void dropFlux(Locks lock) {
        horizonSseStreams.remove(lock);
        log.info("Horizon SSE stream closed because no client");
    }

    @Override
    public void destroy() {
        horizonServer.close();
    }
}