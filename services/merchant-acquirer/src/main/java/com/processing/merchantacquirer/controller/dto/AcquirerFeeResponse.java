package com.processing.merchantacquirer.controller.dto;

import java.math.BigDecimal;

public record AcquirerFeeResponse(
        String stan,
        String pan,
        BigDecimal acquirerFee
) {
}
