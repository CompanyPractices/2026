package com.processing.merchantacquirer.controller.dto;

import java.time.Instant;

public record AcquirerFeeRequest(
        Instant transmissionDateTime,
        String pan,
        String stan,
        Long amount,
        String terminalId
){
}
