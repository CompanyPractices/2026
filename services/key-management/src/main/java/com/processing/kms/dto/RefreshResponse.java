package com.processing.kms.dto;

public record RefreshResponse(
    Boolean isSuccessful,
    String key,
    String failureReason
) {}
