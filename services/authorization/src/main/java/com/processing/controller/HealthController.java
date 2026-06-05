package com.processing.controller;

import com.processing.dto.HealthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

record Response(String service, String status) {
}

@Slf4j
@RestController
@Tag(name = "Health Check", description = "Endpoint for checking service health and dependencies")
public class HealthController {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;

    private final RestTemplate restTemplate;

    public HealthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", description = "Returns the health status of the authorization service and all its dependencies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All services are healthy", content = @Content(schema = @Schema(implementation = HealthResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service is degraded (one or more dependencies are unhealthy)", content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> checks = healthCheckAllServices();
        boolean isAllHealthy = checks.values().stream()
                .allMatch(status -> "ok".equalsIgnoreCase(status));
        HttpStatus httpStatus = isAllHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(new HealthResponse(
                isAllHealthy ? "ok" : "degraded",
                "authorization",
                checks));
    }

    private Map<String, String> healthCheckAllServices() {
        Map<String, String> result = new HashMap<>();
        log.debug("Starting to health check depended services");
        for (String serviceUrl : toHealthCheck) {
            Response healthCheckResponse = checkHealth(serviceUrl);
            result.put(healthCheckResponse.service(), healthCheckResponse.status());
        }
        return result;
    }

    private Response checkHealth(String serviceUrl) {
        try {
            String fullUrl = serviceUrl.startsWith("http") ? serviceUrl : "http://" + serviceUrl;
            String healthUrl = fullUrl + "/health";
            log.debug("Checking health of {}", healthUrl);

            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object statusObj = body.get("status");
                Object serviceObj = body.get("service");
                String status = statusObj instanceof String ? (String) statusObj : "unknown";
                String service = serviceObj instanceof String ? (String) serviceObj : serviceUrl;
                return new Response(service, status);
            }
            return new Response(serviceUrl, "unhealthy");
        } catch (Exception e) {
            log.debug("Health check failed for {}", serviceUrl, e);
            return new Response(serviceUrl, "down");
        }
    }
}
