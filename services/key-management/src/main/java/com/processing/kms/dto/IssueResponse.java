package com.processing.kms.dto;

import com.processing.kms.models.ApiKey;

public record IssueResponse(
        Boolean isSuccessful,
        ApiKey key,
        String failureReason
) {}
