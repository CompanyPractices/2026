package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.service.AuthorizationClient;

public class CapturingAuthorizationClient extends AuthorizationClient {

    private AuthorizationRequest lastRequest;

    public CapturingAuthorizationClient() {
        super(SwitchTestData.defaultProperties(), null);
    }

    @Override
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        lastRequest = request;
        return super.authorize(request);
    }

    public AuthorizationRequest lastRequest() {
        return lastRequest;
    }
}
