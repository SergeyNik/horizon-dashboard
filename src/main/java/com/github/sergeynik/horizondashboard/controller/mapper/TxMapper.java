package com.github.sergeynik.horizondashboard.controller.mapper;

import com.github.sergeynik.horizondashboard.controller.mapper.dto.TxDto;
import org.mapstruct.Mapper;
import org.stellar.sdk.responses.TransactionResponse;

@Mapper(componentModel = "spring")
public interface TxMapper {

    TxDto toTxDto(TransactionResponse source);
}