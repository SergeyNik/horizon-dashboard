package com.github.sergeynik.horizondashboard.controller.mapper.dto;

public record TxDto(String sourceAccount, String createdAt, String memoValue, Long feeCharged, Boolean successful) {}