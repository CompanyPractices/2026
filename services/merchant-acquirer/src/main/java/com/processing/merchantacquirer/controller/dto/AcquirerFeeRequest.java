package com.processing.merchantacquirer.controller.dto;

public record AcquirerFeeRequest(
        String transmissionDateTime,
        String pan,
        String stan,
        Long amount,
        String terminalId
){
}
