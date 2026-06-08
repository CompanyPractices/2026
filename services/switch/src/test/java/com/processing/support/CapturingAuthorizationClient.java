package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.config.RetryFactory;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.service.AuthorizationClient;

public class CapturingAuthorizationClient extends AuthorizationClient {

    private AuthorizationRequest lastRequest;
    private AuthorizationRequest lastReverseOriginal;
    private String lastReverseRrn;
    private boolean reverseCalled;
    private AuthorizationResponse responseToReturn;

    public CapturingAuthorizationClient() {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
        this.responseToReturn = approvedResponse("000001");
    }

    public CapturingAuthorizationClient(AuthorizationResponse responseToReturn) {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
        this.responseToReturn = responseToReturn;
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
    public void reverse(AuthorizationRequest original, String rrn) {
        reverseCalled = true;
        lastReverseOriginal = original;
        lastReverseRrn = rrn;
    }

    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }

    public boolean reverseCalled() {
        return reverseCalled;
    }

    public AuthorizationRequest lastReverseOriginal() {
        return lastReverseOriginal;
    }

    public String lastReverseRrn() {
        return lastReverseRrn;
    }

    private static AuthorizationResponse approvedResponse(String stan) {
        return new AuthorizationResponse(
                "0110", stan, "012345678901", "TEST01", "00", "APPROVED", null, 42);
    }
}
