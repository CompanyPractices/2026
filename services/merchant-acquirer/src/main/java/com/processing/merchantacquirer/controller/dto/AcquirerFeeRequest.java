package com.processing.merchantacquirer.controller.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AcquirerFeeRequest(
        @NotNull
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
