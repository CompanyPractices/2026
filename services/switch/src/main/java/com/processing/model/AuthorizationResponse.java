package com.processing.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationResponse {
    private String mti;
    private String stan;
    private String rrn;
    private String authCode;
    private String responseCode;
    private String status;
    private String declineReason;
    private Integer processingTimeMs;
}
