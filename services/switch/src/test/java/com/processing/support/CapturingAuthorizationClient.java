package com.processing.support;


import com.processing.SwitchTestData;
import com.processing.config.RetryFactory;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.service.AuthorizationClient;


public class CapturingAuthorizationClient extends AuthorizationClient {


    private AuthorizationRequest lastRequest;
    private String lastReverseRrn;
    private boolean reverseCalled;
    private final AuthorizationResponse responseToReturn;


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
        lastReverseRrn = rrn;
    }


    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }


    public boolean reverseCalled() {
        return reverseCalled;
    }


    public String lastReverseRrn() {
        return lastReverseRrn;
    }


    private static AuthorizationResponse approvedResponse() {
        return new AuthorizationResponse(
                "0110", "000001", "012345678901", "TEST01",
                AuthorizationResponse.CODE_APPROVED,
                AuthorizationResponse.STATUS_APPROVED,
                null, 42);
    }
}
