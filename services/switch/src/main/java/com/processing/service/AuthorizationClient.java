package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
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
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> { })
                .body(AuthorizationResponse.class);
        if (response != null
                && (AuthorizationResponse.STATUS_APPROVED.equals(response.status())
                || AuthorizationResponse.STATUS_DECLINED.equals(response.status()))) {
            return response;
        }
        throw new IllegalStateException("Invalid response from Authorization");
    }

    public RollbackResponse rollback(AuthorizationRequest original, String rrn) {
        RollbackRequest request = new RollbackRequest(rrn, original.pan(), original.amount());
        try {
            RollbackResponse response = restClient.post()
                    .uri(switchProperties.authorizationUrl() + "/api/internal/rollback")
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> { })
                    .body(RollbackResponse.class);
            if (response != null
                    && (RollbackResponse.STATUS_APPROVED.equals(response.status())
                    || RollbackResponse.STATUS_DECLINED.equals(response.status()))) {
                return response;
            }
            throw new IllegalStateException("Invalid rollback response from Authorization");
        } catch (Exception e) {
            LOG.error("Rollback failed for STAN={} rrn={}", original.stan(), rrn, e);
            return null;
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
