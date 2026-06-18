package com.processing.kms.service;

import com.processing.common.result.Result;
import com.processing.kms.errors.KeyError;
import com.processing.kms.models.ApiKeyRole;
import com.processing.kms.models.ApiKey;
import com.processing.kms.properties.KmsProperties;
import com.processing.kms.repository.ApiKeyRepository;
import com.processing.kms.utils.ApiKeyGenerator;
import com.processing.kms.utils.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final KmsProperties kmsProperties;

    private final Map<String, ApiKeyRole> clientToRoleMap = new HashMap<>();

    private int ttl;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        kmsProperties.getClients().forEach((client) ->
                clientToRoleMap.putIfAbsent(client.getType(), client.getRole()));

        ttl = kmsProperties.getTtl();
    }

    public Result<ApiKey, KeyError> issueKey(String clientType) {
        ApiKeyRole role = clientToRoleMap.get(clientType);

//        ApiKey duplicateKey = apiKeyRepository.getByOwnerId(clientType);
//        if (duplicateKey != null && !duplicateKey.getIsExpired()) {
//            return new Result.Failure<>(new KeyError.OwnerAlreadyHasKey());
//        }

        return new Result.Success<>(addNewKey(clientType, role));
    }

    public Result<ApiKeyRole, KeyError> validateKey(String key) {
        String encodedKey = ApiKeyHasher.hash(key);
        ApiKey apiKey = apiKeyRepository.get(encodedKey);

        if (apiKey == null) {
            return new Result.Failure<>(new KeyError.NotFound());
        }

        if (apiKey.getIsExpired()) {
            return new Result.Failure<>(new KeyError.Expired());
        }

        if (apiKey.getExpiresBy().isBefore(Instant.now())) {
            apiKey.setIsExpired(true);
            apiKeyRepository.addOrUpdate(apiKey);

            return new Result.Failure<>(new KeyError.Expired());
        }

        return new Result.Success<>(apiKey.getRole());
    }

    public Result<ApiKey, KeyError> refreshKey(String ownerId, String oldKey) {
        String encodedKey = ApiKeyHasher.hash(oldKey);
        ApiKey oldApiKey = apiKeyRepository.get(encodedKey);

        if (oldApiKey == null) {
            return new Result.Failure<>(new KeyError.NotFound());
        }

        if (!oldApiKey.getOwnerId().equals(ownerId)) {
            return new Result.Failure<>(new KeyError.OwnerIdMismatch());
        }

        oldApiKey.setIsExpired(true);
        apiKeyRepository.addOrUpdate(oldApiKey);

        return new Result.Success<>(addNewKey(ownerId, oldApiKey.getRole()));
    }

    private ApiKey addNewKey(String ownerId, ApiKeyRole role) {
        String rawKey = apiKeyGenerator.generate();
        String encodedKey = ApiKeyHasher.hash(rawKey);

        var key = new ApiKey(
                encodedKey,
                ownerId,
                role,
                Instant.now(),
                Instant.now().plus(ttl, ChronoUnit.MINUTES),
                false
        );
        apiKeyRepository.addOrUpdate(key);
        key.setKey(rawKey);

        return key;
    }
}
