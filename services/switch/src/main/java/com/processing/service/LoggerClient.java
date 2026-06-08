package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.LoggerException;
import com.processing.model.LogResponse;
import com.processing.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LoggerClient {

    private static final long[] BACKOFF_MS = {1000, 2000, 4000};
    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;

    public LoggerClient(
            SwitchProperties switchProperties,
            @Qualifier("loggerRestClient") RestClient restClient) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
    }

    public boolean log(Transaction transaction) {
        int maxAttempts = switchProperties.retry().maxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LogResponse response = restClient.post()
                        .uri(switchProperties.loggerUrl() + "/api/internal/log")
                        .body(transaction)
                        .retrieve()
                        .body(LogResponse.class);
                LOG.info("Logger stored TX {} id={}", transaction.stan(),
                        response != null ? response.id() : transaction.id());
                return true;
            } catch (Exception e) {
                LOG.warn("Logger attempt {}/{} failed for TX {}: {}",
                        attempt, maxAttempts, transaction.stan(), e.getMessage());
                if (attempt < maxAttempts) {
                    sleep(BACKOFF_MS[attempt - 1]);
                }
            }
        }

        throw new LoggerException(transaction.stan(), transaction.id(), maxAttempts);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}