package com.processing.authorization.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

record Response(String service, String status) {
}

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;
    private final RestTemplate restTemplate;

    public Map<String, String> healthCheckAllServices() {
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

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    return new Response(serviceUrl, "unknown");
                }
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
