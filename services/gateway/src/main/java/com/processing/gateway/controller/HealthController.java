package com.processing.gateway.controller;

import com.processing.gateway.dto.HealthResponse;
import com.processing.gateway.enums.HealthStatus;
import com.processing.gateway.service.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes gateway health information
 */
@RestController
@Slf4j
public class HealthController {
    private final HealthService healthService;

    /**
     * Creates a health controller
     *
     * @param healthService service that checks downstream services availability
     */
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Returns health status for the gateway and configured downstream services
     *
     * @return gateway health response
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = healthService.getDownstreamServicesHealth();

        return health.status() == HealthStatus.OK
                ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }
}
