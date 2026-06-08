package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
        int maxAttempts = switchProperties.retryOrDefaults().maxAttempts();
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
                throw new IllegalStateException("Empty response from Authorization");
            } catch (RestClientResponseException e) {
                AuthorizationResponse body = e.getResponseBodyAs(AuthorizationResponse.class);
                if (body != null) {
                    return body;
                }
                LOG.warn("Authorization HTTP error for STAN={} (attempt {}/{}): {}",
                        request.stan(), attempt, maxAttempts, e.getMessage());
            } catch (Exception e) {
                LOG.warn("Authorization unavailable for STAN={} (attempt {}/{}): {}",
                        request.stan(), attempt, maxAttempts, e.getMessage());
            }
        }
        LOG.error("Authorization service unavailable for STAN={} after {} attempts",
                request.stan(), maxAttempts);
        return AuthorizationResponse.authUnavailable(request.stan());
    }

    public void reverse(AuthorizationRequest original) {
        AuthorizationRequest reversal = new AuthorizationRequest(
                "0400",
                original.stan(),
                original.pan(),
                original.processingCode(),
                original.amount(),
                original.currencyCode(),
                original.transmissionDateTime(),
                original.terminalId(),
                original.terminalType(),
                original.merchantId(),
                original.mcc(),
                original.acquirerId(),
                original.issuerId()
        );
        int maxAttempts = switchProperties.retryOrDefaults().maxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                        .body(reversal)
                        .retrieve()
                        .toBodilessEntity();
                LOG.info("Reversal sent for STAN={} (attempt {})", original.stan(), attempt);
                return;
            } catch (RestClientResponseException e) {
                LOG.warn("Reversal HTTP error for STAN={} (attempt {}/{}): {}",
                        original.stan(), attempt, maxAttempts, e.getMessage());
                return;
            } catch (Exception e) {
                LOG.warn("Reversal failed for STAN={} (attempt {}/{}): {}",
                        original.stan(), attempt, maxAttempts, e.getMessage());
            }
        }
        LOG.error("Reversal failed for STAN={} after {} attempts", original.stan(), maxAttempts);
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
