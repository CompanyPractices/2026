package com.processing.merchantacquirer.controller.dto;

public record AcquirerFeeResponse(
        String transmissionDateTime,
        String stan,
        Long acquirerFee
) {
}
