package com.processing.kms.dto;

public record RefreshResponse(
    RefreshOutcome outcome,
    String key,
    String failureReason
) {}
