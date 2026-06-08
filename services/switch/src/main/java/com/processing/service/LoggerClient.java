package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.model.LogResponse;
import com.processing.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LoggerClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);
    private static final long[] BACKOFF_MS = {1000, 2000, 4000};

    private final SwitchProperties switchProperties;
    private final RestClient loggerRestClient;

    public LoggerClient(
            SwitchProperties switchProperties,
            @Qualifier("loggerRestClient") RestClient loggerRestClient) {
        this.switchProperties = switchProperties;
        this.loggerRestClient = loggerRestClient;
    }

    public boolean log(Transaction transaction) {
        int maxAttempts = switchProperties.retryOrDefaults().maxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LogResponse response = loggerRestClient.post()
                        .uri(switchProperties.loggerUrl() + "/api/internal/log")
                        .body(transaction)
                        .retrieve()
                        .body(LogResponse.class);
                LOG.info("Logger stored TX {} id={}", transaction.stan(),
                        response != null ? response.id() : transaction.id());
                return true;
            } catch (Exception e) {
                LOG.warn("Logger unavailable for TX {} (attempt {}/{}): {}",
                        transaction.stan(), attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        return false;
    }

    private static void sleepBeforeRetry(int attempt) {
        long delayMs = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
