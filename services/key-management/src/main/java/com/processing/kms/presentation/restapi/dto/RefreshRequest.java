package com.processing.kms.presentation.restapi.dto;

public record RefreshRequest(
        String clientId,
        String oldKey
) {}
