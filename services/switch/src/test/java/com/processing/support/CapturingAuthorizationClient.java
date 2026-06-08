package com.processing.support;

import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.service.AuthorizationClient;

public class CapturingAuthorizationClient extends AuthorizationClient {

    private AuthorizationRequest lastRequest;
    private AuthorizationResponse responseToReturn;
    private boolean reverseCalled;
    private AuthorizationRequest lastReversalRequest;

    public CapturingAuthorizationClient() {
        super(com.processing.SwitchTestData.defaultProperties(), null);
        this.responseToReturn = approvedFixture("000001");
    }

    public CapturingAuthorizationClient withResponse(AuthorizationResponse response) {
        this.responseToReturn = response;
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
                responseToReturn.processingTimeMs()
        );
    }

    @Override
    public void reverse(AuthorizationRequest original) {
        reverseCalled = true;
        lastReversalRequest = new AuthorizationRequest(
                "0400",
                original.stan(),
                original.pan(),
                original.processingCode(),
                original.amount(),
                original.currencyCode(),
                original.transmissionDateTime(),
                original.terminalId(),
                original.terminalType(),
                original.merchantId(),
                original.mcc(),
                original.acquirerId(),
                original.issuerId()
        );
    }

    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }

    public boolean reverseCalled() {
        return reverseCalled;
    }

    public AuthorizationRequest lastReversalRequest() {
        return lastReversalRequest;
    }

    public static AuthorizationResponse approvedFixture(String stan) {
        return new AuthorizationResponse(
                "0110", stan, "012345678901", "TEST01", "00", "APPROVED", null, 42);
    }

    public static AuthorizationResponse declinedFixture(String stan, String responseCode) {
        return new AuthorizationResponse(
                "0110", stan, null, null, responseCode, "DECLINED", "Declined", 10);
    }
}
