package com.processing.kms.service;

import com.processing.common.result.Result;
import com.processing.kms.errors.KeyError;
import com.processing.kms.models.ApiKeyRoles;
import com.processing.kms.models.ApiKey;
import com.processing.kms.storage.ApiKeyStorage;
import com.processing.kms.utils.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    @Value("${api-keys.ttl-min:60}")
    private int ttl;

    private final ApiKeyStorage apiKeyStorage;
    private final ApiKeyGenerator apiKeyGenerator;

    public Result<ApiKey, KeyError> issueKey(String ownerId, ApiKeyRoles role) {
        if (apiKeyStorage.getByOwnerId() != null) {
            return new Result.Failure<>(new KeyError.OwnerAlreadyHasKey());
        }

        return new Result.Success<>(addNewKey(ownerId, role));
    }

    public Result<ApiKeyRoles, KeyError> validateKey(String key) {
        ApiKey apiKey = apiKeyStorage.get(key);

        if (apiKey == null) {
            return new Result.Failure<>(new KeyError.NotFound());
        }

        if (apiKey.expiresBy().isBefore(Instant.now())) {
            return new Result.Failure<>(new KeyError.Expired());
        }

        return new Result.Success<>(apiKey.role());
    }

    public Result<ApiKey, KeyError> refreshKey(String ownerId, String oldKey) {
        ApiKey oldApiKey = apiKeyStorage.get(oldKey);

        if (oldApiKey == null) {
            return new Result.Failure<>(new KeyError.NotFound());
        }

        if (!oldApiKey.ownerId().equals(ownerId)) {
            return new Result.Failure<>(new KeyError.OwnerIdMismatch());
        }

        return new Result.Success<>(addNewKey(ownerId, oldApiKey.role()));
    }

    private ApiKey addNewKey(String ownerId, ApiKeyRoles role) {
        var key = new ApiKey(
                apiKeyGenerator.generate(),
                ownerId,
                role,
                Instant.now(),
                Instant.now().plus(ttl, ChronoUnit.MINUTES)
        );
        apiKeyStorage.add(key);

        return key;
    }
}
