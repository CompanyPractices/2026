package com.processing.gateway.dto;

import com.processing.gateway.enums.HealthStatus;

import java.util.Map;

/**
 * Health payload returned by the gateway health endpoint
 *
 * @param status gateway status
 * @param service service name
 * @param version service version
 * @param services downstream service health statuses by service name
 */
public record HealthResponse(
        HealthStatus status,
        String service,
        String version,
        Map<String, HealthStatus> services
) {}
