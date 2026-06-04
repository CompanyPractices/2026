package com.processing.controller;

import com.processing.dto.HealthResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
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
public class HealthController {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;

    private final RestTemplate restTemplate;

    public HealthController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {

        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "authorization",
                healthCheckAllServices()));
    }

    private Map<String, String> healthCheckAllServices() {
        Map<String, String> result = new HashMap<>();
        // System.out.println("[DEBUG] Services to check: " + toHealthCheck);
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

            if (response.getBody() != null && response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                String service = (String) body.getOrDefault("service", serviceUrl);
                String status = (String) body.getOrDefault("status", "unknown");
                return new Response(service, status);
            }
            return new Response(serviceUrl, "unhealthy");
        } catch (Exception e) {
            log.error("Health check failed for {}", serviceUrl, e);
            return new Response(serviceUrl, "down");
        }
    }
}
