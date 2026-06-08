package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthorizationClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;

    public AuthorizationClient(SwitchProperties switchProperties, RestClient restClient) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
    }

    public AuthorizationResponse authorize(AuthorizationRequest request) {
        int maxAttempts = switchProperties.retry().maxAttempts();
        String lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AuthorizationResponse response = restClient.post()
                        .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                        .body(request)
                        .retrieve()
                        .body(AuthorizationResponse.class);
                if (response != null) {
                    return response;
                }
                lastError = "Empty response from Authorization";
            } catch (Exception e) {
                lastError = e.getMessage();
                LOG.warn("Authorization attempt {}/{} failed for STAN={}: {}",
                        attempt, maxAttempts, request.stan(), e.getMessage());
            }
        }

        throw new AuthorizationException(request.stan(), maxAttempts, lastError);
    }

    public void reverse(AuthorizationRequest original, String rrn) {
        AuthorizationRequest reversal = original.forReversal(rrn);
        try {
            restClient.post()
                    .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                    .body(reversal)
                    .retrieve()
                    .toBodilessEntity();
            LOG.info("Reversal sent for STAN={} rrn={}", original.stan(), rrn);
        } catch (Exception e) {
            LOG.error("Reversal failed for STAN={} rrn={}: {}", original.stan(), rrn, e.getMessage());
        }
    }

    public String checkHealth() {
        try {
            restClient.get()
                    .uri(switchProperties.authorizationUrl() + "/health")
                    .retrieve()
                    .toBodilessEntity();
            return "ok";
        } catch (Exception e) {
            LOG.warn("Authorization health check failed: {}", e.getMessage());
            return "down";
        }
    }
}