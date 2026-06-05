package com.processing.terminalsimulator.dto;


public record AuthorizationResponse(
    String mti,
    String stan,
    String rrn,
    String authCode,
    String responseCode,
    String status,
    String declineReason,
    long processingTimeMs
) { }
