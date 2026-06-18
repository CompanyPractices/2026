package com.processing.kms.presentation.restapi.dto;

import com.processing.kms.domain.models.ApiKeyRole;

public record ValidationResponse(
    Boolean isValid,
    ApiKeyRole role,
    String invalidationReason
) {}
