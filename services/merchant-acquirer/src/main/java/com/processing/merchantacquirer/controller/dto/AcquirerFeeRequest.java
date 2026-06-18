package com.processing.merchantacquirer.controller.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AcquirerFeeRequest(
        @NotBlank
        Instant transmissionDateTime,
        @NotBlank
        String pan,
        @NotBlank
        String stan,
        BigDecimal amount,
        @NotBlank
        String terminalId
){
}
