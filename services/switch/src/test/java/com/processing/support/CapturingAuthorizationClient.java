package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.RetryFactory;
import com.processing.service.AuthorizationClient;

import java.time.Instant;

public class CapturingAuthorizationClient extends AuthorizationClient {

    private AuthorizationRequest lastRequest;
    private RollbackRequest lastRollbackRequest;
    private String lastRollbackRrn;
    private boolean rollbackCalled;
    private final AuthorizationResponse responseToReturn;
    private RollbackResponse rollbackResponseToReturn;

    public CapturingAuthorizationClient() {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
        this.responseToReturn = approvedResponse();
    }

    public CapturingAuthorizationClient(AuthorizationResponse responseToReturn) {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
        this.responseToReturn = responseToReturn;
    }

    public CapturingAuthorizationClient withRollbackResponse(RollbackResponse rollbackResponse) {
        this.rollbackResponseToReturn = rollbackResponse;
        return this;
    }

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

    @Override
    public RollbackResponse rollback(AuthorizationRequest original, String rrn) {
        rollbackCalled = true;
        lastRollbackRrn = rrn;
        lastRollbackRequest = new RollbackRequest(rrn, original.pan(), original.amount());
        if (rollbackResponseToReturn != null) {
            return rollbackResponseToReturn;
        }
        return RollbackResponse.approved(rrn, Instant.now());
    }

    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }

    public boolean rollbackCalled() {
        return rollbackCalled;
    }

    public String lastRollbackRrn() {
        return lastRollbackRrn;
    }

    public RollbackRequest lastRollbackRequest() {
        return lastRollbackRequest;
    }

    private static AuthorizationResponse approvedResponse() {
        return new AuthorizationResponse(
                "0110", "000001", "012345678901", "TEST01",
                AuthorizationResponse.CODE_APPROVED,
                AuthorizationResponse.STATUS_APPROVED,
                null, 42);
    }
}
