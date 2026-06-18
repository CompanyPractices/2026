package com.processing.merchantacquirer.controller.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AcquirerFeeRequest(
        @NotBlank
        String transmissionDateTime,
        @NotBlank
        String pan,
        @NotBlank
        String stan,
        BigDecimal amount,
        @NotBlank
        String terminalId
){
}
