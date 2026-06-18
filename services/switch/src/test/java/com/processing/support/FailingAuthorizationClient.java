package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.config.RetryFactory;
import com.processing.exception.AuthorizationException;
import com.processing.service.AuthorizationClient;

public class FailingAuthorizationClient extends AuthorizationClient {

    public FailingAuthorizationClient() {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
    }

    @Override
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        throw new AuthorizationException(request.stan(), 3, "unavailable");
    }
}
