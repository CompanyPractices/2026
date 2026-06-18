package com.processing.kms.presentation.restapi.dto;

public record RefreshResponse(
    Boolean isSuccessful,
    String key,
    String failureReason
) {}
