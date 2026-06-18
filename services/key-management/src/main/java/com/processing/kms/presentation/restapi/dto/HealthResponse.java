package com.processing.kms.presentation.restapi.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        Map<String, String> dependencies
) {}
