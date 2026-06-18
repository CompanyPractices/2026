package com.processing.service;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.config.SwitchProperties;
import com.processing.exception.LoggerException;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент для синхронной отправки транзакций в Transaction Logger.
 */
@Service
public class LoggerClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;
    private final Retry loggerRetry;

    /**
     * @param switchProperties конфигурация URL и retry
     * @param restClient       REST-клиент с таймаутами для Logger
     * @param loggerRetry      политика повторных попыток с exponential backoff
     */
    public LoggerClient(
            SwitchProperties switchProperties,
            @Qualifier("loggerRestClient") RestClient restClient,
            @Qualifier("loggerRetry") Retry loggerRetry) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
        this.loggerRetry = loggerRetry;
    }

    /**
     * Сохраняет транзакцию в Logger с retry при временных сбоях.
     *
     * @param transaction полный объект транзакции
     * @return {@code true} при успешной записи
     * @throws LoggerException если все попытки исчерпаны
     */
    public boolean log(TransactionRequest transaction) {
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

    /**
     * Выполняет один HTTP-вызов {@code POST /api/internal/log}.
     *
     * @param transaction тело запроса
     * @return ответ Logger с подтверждением записи
     */
    private TransactionStoredResponse sendToLogger(TransactionRequest transaction) {
        TransactionStoredResponse response = restClient.post()
                .uri(switchProperties.loggerUrl() + "/api/internal/log")
                .body(transaction)
                .retrieve()
                .body(TransactionStoredResponse.class);
        LOG.info("Logger stored TX {} id={}", transaction.stan(),
                response != null ? response.id() : transaction.id());
        return response;
    }
}
