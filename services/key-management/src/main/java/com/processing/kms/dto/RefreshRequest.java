package com.processing.kms.dto;

public record RefreshRequest(
        String clientId,
        String oldKey
) {}
