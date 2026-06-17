package com.processing.kms.dto;

import com.processing.kms.models.ApiKeyRoles;

public record ValidationResponse(
    Boolean isValid,
    ApiKeyRoles role,
    String invalidationReason
) {}
