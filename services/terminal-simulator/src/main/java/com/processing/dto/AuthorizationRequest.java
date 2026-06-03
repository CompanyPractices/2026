package com.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizationRequest {
    private String mti;
    private String stan;
    private String pan;
    private String processingCode;
    private long amount;
    private String currencyCode;
    private String transmissionDateTime;
    private String terminalId;
    private String terminalType;
    private String merchantId;
    private String mcc;
    private String acquirerId;
    private String issuerId;
}
