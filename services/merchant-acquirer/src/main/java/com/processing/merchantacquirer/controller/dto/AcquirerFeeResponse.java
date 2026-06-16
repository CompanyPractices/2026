package com.processing.merchantacquirer.controller.dto;

import java.math.BigDecimal;

public record AcquirerFeeResponse(
        BigDecimal acquirerFee
) {
}
