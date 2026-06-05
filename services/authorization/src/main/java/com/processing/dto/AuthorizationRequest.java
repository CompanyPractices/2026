package com.processing.dto;

import java.time.LocalDateTime;
import com.processing.enums.TerminalType;
import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class AuthorizationRequest {
    private String mti;

    private String stan;

    private String pan;

    private String processingCode;

    private Integer amount;

    private String currencyCode;

    private LocalDateTime transmissionDateTime;

    private String terminalId;

    private TerminalType terminalType;

    private String merchantId;

    private String mcc;

    private String acquirerId;

    private String issuerId;
}
