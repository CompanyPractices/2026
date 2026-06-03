package com.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationResponse {
    private String mti;
    private String stan;
    private String rrn;
    private String authCode;
    private String responseCode;
    private String status;
    private String declineReason;
    private long processingTimeMs;
}
