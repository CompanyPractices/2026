package com.processing.service;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.config.SwitchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LoggerClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;

    public LoggerClient(SwitchProperties switchProperties, RestClient restClient) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
    }

    public boolean log(TransactionRequest transaction) {
        // POST /api/internal/log
        try {
            TransactionStoredResponse response = restClient.post()
                    .uri(switchProperties.loggerUrl() + "/api/internal/log")
                    .body(transaction)
                    .retrieve()
                    .body(TransactionStoredResponse.class);
            LOG.info("Logger stored TX {} id={}", transaction.stan(),
                    response != null ? response.id() : transaction.id());
            return true;
        } catch (Exception e) {
            LOG.warn("Logger unavailable for TX {}: {}", transaction.stan(), e.getMessage());
            // retry 3 раза, reversal mti=0400, DECLINED responseCode=96
            return false;
        }
    }
}
