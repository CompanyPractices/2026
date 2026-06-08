package com.processing.service;


import com.processing.config.SwitchProperties;
import com.processing.exception.LoggerException;
import com.processing.common.dto.transaction.LogResponse;
import com.processing.common.dto.transaction.Transaction;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class LoggerClient {


    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);


    private final SwitchProperties switchProperties;
    private final RestClient restClient;
    private final Retry loggerRetry;


    public LoggerClient(
            SwitchProperties switchProperties,
            @Qualifier("loggerRestClient") RestClient restClient,
            @Qualifier("loggerRetry") Retry loggerRetry) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
        this.loggerRetry = loggerRetry;
    }


    public boolean log(Transaction transaction) {
        int maxAttempts = switchProperties.retry().maxAttempts();
        try {
            Retry.decorateCallable(loggerRetry, () -> sendToLogger(transaction)).call();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LoggerException(transaction.stan(), transaction.id(), maxAttempts);
        } catch (Exception e) {
            throw new LoggerException(transaction.stan(), transaction.id(), maxAttempts);
        }
    }


    private LogResponse sendToLogger(Transaction transaction) {
        LogResponse response = restClient.post()
                .uri(switchProperties.loggerUrl() + "/api/internal/log")
                .body(transaction)
                .retrieve()
                .body(LogResponse.class);
        LOG.info("Logger stored TX {} id={}", transaction.stan(),
                response != null ? response.id() : transaction.id());
        return response;
    }
}
