package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.config.RetryFactory;
import com.processing.exception.AuthorizationException;
import com.processing.service.AuthorizationClient;

/**
 * Тестовый double {@link AuthorizationClient}, всегда бросающий {@link AuthorizationException}.
 */
public class FailingAuthorizationClient extends AuthorizationClient {

    /** Создаёт клиент, имитирующий недоступность Authorization. */
    public FailingAuthorizationClient() {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
    }

    /** {@inheritDoc} */
    @Override
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        throw new AuthorizationException(request.stan(), 3, "unavailable");
    }
}
