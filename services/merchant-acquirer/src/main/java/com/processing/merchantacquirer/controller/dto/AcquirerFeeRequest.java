package com.processing.merchantacquirer.controller.dto;

import java.math.BigDecimal;

public record AcquirerFeeRequest(
        String transmissionDateTime,
        String pan,
        String stan,
        BigDecimal amount,
        String terminalId
){
}
