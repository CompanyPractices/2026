package com.processing.service;


import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class AuthorizationClient {


    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationClient.class);


    private final SwitchProperties switchProperties;
    private final RestClient restClient;
    private final Retry authorizationRetry;


    public AuthorizationClient(
            SwitchProperties switchProperties,
            RestClient restClient,
            @Qualifier("authorizationRetry") Retry authorizationRetry) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
        this.authorizationRetry = authorizationRetry;
    }


    public AuthorizationResponse authorize(AuthorizationRequest request) {
        int maxAttempts = switchProperties.retry().maxAttempts();
        try {
            return Retry.decorateCallable(authorizationRetry, () -> callAuthorize(request)).call();
        } catch (Exception e) {
            String lastError = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new AuthorizationException(request.stan(), maxAttempts, lastError);
        }
    }


    private AuthorizationResponse callAuthorize(AuthorizationRequest request) {
        AuthorizationResponse response = restClient.post()
                .uri(switchProperties.authorizationUrl() + "/api/internal/authorize")
                .body(request)
                .retrieve()
                .body(AuthorizationResponse.class);
        if (response != null) {
            return response;
        }
        throw new IllegalStateException("Empty response from Authorization");
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
            LOG.error("Reversal failed for STAN={} rrn={}", original.stan(), rrn, e);
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
            LOG.warn("Authorization health check failed", e);
            return "down";
        }
    }
}
