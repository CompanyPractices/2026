package com.processing.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        String version,
        Map<String, String> dependencies
) {}
