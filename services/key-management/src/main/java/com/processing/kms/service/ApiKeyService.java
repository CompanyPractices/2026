package com.processing.kms.service;

import com.processing.kms.result.errors.KeyError;
import com.processing.kms.models.ApiKeyRoles;
import com.processing.kms.models.ApiKey;
import com.processing.kms.result.Result;
import com.processing.kms.storage.ApiKeyStorage;
import com.processing.kms.utils.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    @Value("${api-keys.ttl-min:60}")
    private int ttl;

    private final ApiKeyStorage apiKeyStorage;
    private final ApiKeyGenerator apiKeyGenerator;

    public ApiKey issueKey(ApiKeyRoles role) {
        var key = new ApiKey(
                apiKeyGenerator.generate(),
                role,
                Instant.now(),
                Instant.now().plus(ttl, ChronoUnit.MINUTES)
        );

        apiKeyStorage.add(key);

        return key;
    }

    public Result<ApiKeyRoles, KeyError> validateKey(String key) {
        ApiKey apiKey = apiKeyStorage.get(key);

        if (apiKey == null) {
            return new Result.Failure<>(new KeyError.KeyNotFoundError());
        }

        if (apiKey.expiresBy().isBefore(Instant.now())) {
            return new Result.Failure<>(new KeyError.KeyExpiredError());
        }

        return new Result.Success<>(apiKey.role());
    }

//    public ApiKey refreshKey() {
//
//    }
}
