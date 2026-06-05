package com.processing.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuthorizationRequest {
    private String mti;
    private String stan;
    private String pan;
    private String processingCode;
    private Long amount;
    private String currencyCode;
    private LocalDateTime transmissionDateTime;
    private String terminalId;
    private String terminalType;
    private String merchantId;
    private String mcc;
    private String acquirerId;
    private String issuerId;
}
