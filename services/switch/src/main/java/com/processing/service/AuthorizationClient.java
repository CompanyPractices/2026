package com.processing.service;

import com.processing.config.SwitchProperties;
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
        if (switchProperties.authorizationStubEnabled()) {
            return stubApprove(request);
        }

        // POST /api/internal/authorize
        try {
            AuthorizationResponse response = restClient.post()
                    .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                    .body(request)
                    .retrieve()
                    .body(AuthorizationResponse.class);
            if (response != null) {
                return response;
            }
            throw new IllegalStateException("Empty response from Authorization");
        } catch (Exception e) {
            LOG.error("Authorization service unavailable for STAN={}: {}", request.stan(), e.getMessage());
            // retry 3 попытки, затем DECLINED responseCode=05
            return AuthorizationResponse.authUnavailable(request.stan());
        }
    }

    public String checkHealth() {
        if (switchProperties.authorizationStubEnabled()) {
            return "ok";
        }

        // GET /health
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

    private AuthorizationResponse stubApprove(AuthorizationRequest request) {
        LOG.debug("Authorization stub: STAN={} issuerId={}", request.stan(), request.issuerId());
        return AuthorizationResponse.stubApproved(request.stan());
    }
}
