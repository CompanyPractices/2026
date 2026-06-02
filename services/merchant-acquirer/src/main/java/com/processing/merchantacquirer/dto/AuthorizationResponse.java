package com.processing.merchantacquirer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizationResponse {
    @NotNull
    private String mti;
    @NotNull
    private String stan;
    @NotNull
    private String pan;
    @NotNull
    private String processingCode;
    @NotBlank
    private int amount;
    @NotNull
    private String currencyCode;
    @NotNull
    private String transmissionDateTime;
    @NotNull
    private String terminalId;
    private String terminalType;
    @NotNull
    private String merchantId;
    @NotNull
    private String mcc;
    private String acquirerId;
}
