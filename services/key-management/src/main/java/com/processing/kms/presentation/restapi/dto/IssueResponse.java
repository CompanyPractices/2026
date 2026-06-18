package com.processing.kms.presentation.restapi.dto;

import com.processing.kms.domain.models.ApiKey;

public record IssueResponse(
        Boolean isSuccessful,
        ApiKey key,
        String failureReason
) {}
