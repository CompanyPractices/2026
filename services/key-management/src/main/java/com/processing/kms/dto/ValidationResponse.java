package com.processing.kms.dto;

import com.processing.kms.models.ApiKeyRoles;

public record ValidationResponse(
    ValidationOutcome outcome,
    ApiKeyRoles role,
    String invalidationReason
) {}
