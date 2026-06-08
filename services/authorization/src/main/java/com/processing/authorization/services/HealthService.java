package com.processing.authorization.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

record Response(String service, String status) {
}

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    @Value("${services-to-health-check}")
    private List<String> toHealthCheck;
    private final WebClient webClient;

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

            Map<String, Object> body = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.debug("Failed to get card. Status: {}", clientResponse.statusCode());
                        return Mono
                                .error(new RuntimeException(
                                        "Failed to get card. Status: " + clientResponse.statusCode()));
                    })
                    .bodyToMono(Map.class)
                    .block();

            if (body == null) {
                return new Response(serviceUrl, "unknown");
            }

            Object statusObj = body.get("status");
            Object serviceObj = body.get("service");
            String status = statusObj instanceof String ? (String) statusObj : "unknown";
            String service = serviceObj instanceof String ? (String) serviceObj : serviceUrl;
            return new Response(service, status);
        } catch (Exception e) {
            log.debug("Health check failed for {}", serviceUrl, e);
            return new Response(serviceUrl, "down");
        }
    }
}
