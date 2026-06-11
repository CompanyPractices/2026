package com.processing.gateway.controller;

import com.processing.gateway.dto.HealthResponse;
import com.processing.gateway.enums.HealthStatus;
import com.processing.gateway.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing gateway health information.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    private final HealthService healthService;

    /**
     * Returns gateway health together with downstream service statuses.
     *
     * @return HTTP 200 when all dependencies are available, otherwise HTTP 503
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = healthService.getDownstreamServicesHealth();

        return health.status() == HealthStatus.OK
                ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }
}
