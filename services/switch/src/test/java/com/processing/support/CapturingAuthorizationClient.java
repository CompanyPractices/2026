package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.CircuitBreakerFactory;
import com.processing.config.RetryFactory;
import com.processing.service.AuthorizationClient;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Тестовый double {@link AuthorizationClient}, записывающий последние вызовы authorize/rollback.
 */
public class CapturingAuthorizationClient extends AuthorizationClient {

    private AuthorizationRequest lastRequest;
    private RollbackRequest lastRollbackRequest;
    private String lastRollbackRrn;
    private boolean rollbackCalled;
    private final AuthorizationResponse responseToReturn;
    private RollbackResponse rollbackResponseToReturn;

    /** Создаёт клиент, возвращающий APPROVED по умолчанию. */
    public CapturingAuthorizationClient() {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()),
                CircuitBreakerFactory.authorizationCircuitBreaker(SwitchTestData.defaultProperties()));
        this.responseToReturn = approvedResponse();
    }

    /**
     * @param responseToReturn фиксированный ответ authorize
     */
    public CapturingAuthorizationClient(AuthorizationResponse responseToReturn) {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()),
                CircuitBreakerFactory.authorizationCircuitBreaker(SwitchTestData.defaultProperties()));
        this.responseToReturn = responseToReturn;
    }

    /**
     * Задаёт ответ rollback для следующих вызовов.
     *
     * @param rollbackResponse ответ Authorization на rollback
     * @return {@code this} для цепочки вызовов
     */
    public CapturingAuthorizationClient withRollbackResponse(RollbackResponse rollbackResponse) {
        this.rollbackResponseToReturn = rollbackResponse;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        lastRequest = request;
        return new AuthorizationResponse(
                responseToReturn.mti(),
                request.stan(),
                responseToReturn.rrn(),
                responseToReturn.authCode(),
                responseToReturn.responseCode(),
                responseToReturn.status(),
                responseToReturn.declineReason(),
                responseToReturn.processingTimeMs());
    }

    /** {@inheritDoc} */
    @Override
    public RollbackResponse rollback(AuthorizationRequest original, String rrn) {
        rollbackCalled = true;
        lastRollbackRrn = rrn;
        lastRollbackRequest = new RollbackRequest(rrn, original.pan(), original.amount());
        if (rollbackResponseToReturn != null) {
            return rollbackResponseToReturn;
        }
        return RollbackResponse.approved(new RollbackRequest(rrn, lastRequest().pan(), BigDecimal.valueOf(5000)), Instant.now());
    }

    /**
     * @return последний запрос, переданный в {@link #authorize}
     */
    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }

    /**
     * @return {@code true}, если {@link #rollback} был вызван
     */
    public boolean rollbackCalled() {
        return rollbackCalled;
    }

    /**
     * @return RRN последнего rollback-вызова
     */
    public String lastRollbackRrn() {
        return lastRollbackRrn;
    }

    /**
     * @return тело последнего rollback-запроса
     */
    public RollbackRequest lastRollbackRequest() {
        return lastRollbackRequest;
    }

    /**
     * @return ответ APPROVED для тестов по умолчанию
     */
    private static AuthorizationResponse approvedResponse() {
        return new AuthorizationResponse(
                "0110", "000001", "012345678901", "TEST01",
                AuthorizationResponse.CODE_APPROVED,
                AuthorizationResponse.STATUS_APPROVED,
                null, 42);
    }
}
