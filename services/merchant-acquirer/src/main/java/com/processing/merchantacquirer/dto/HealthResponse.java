package com.processing.merchantacquirer.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        Map<String, String> dependencies
) {}
