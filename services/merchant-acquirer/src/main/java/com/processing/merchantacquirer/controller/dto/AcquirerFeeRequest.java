package com.processing.merchantacquirer.controller.dto;

import java.time.Instant;

import java.math.BigDecimal;

public record AcquirerFeeRequest(
        Instant transmissionDateTime,
        String pan,
        String stan,
        BigDecimal amount,
        String terminalId
){
}
