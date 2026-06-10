package com.processing.gateway.dto;

import com.processing.gateway.enums.HealthStatus;
import lombok.Getter;

import java.util.Map;

public record HealthResponse(
        HealthStatus status,
        String service,
        String version,
        Map<String, HealthStatus> services
) {}
