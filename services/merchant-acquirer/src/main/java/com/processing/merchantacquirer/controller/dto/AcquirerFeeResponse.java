package com.processing.merchantacquirer.controller.dto;

public record AcquirerFeeResponse(
        String stan,
        String pan,
        double acquirerFee
) {
}
