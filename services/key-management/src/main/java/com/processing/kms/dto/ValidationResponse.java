package com.processing.kms.dto;

import com.processing.kms.models.ApiKeyRole;

public record ValidationResponse(
    Boolean isValid,
    ApiKeyRole role,
    String invalidationReason
) {}
