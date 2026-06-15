package com.processing.gateway.service;

import com.processing.gateway.enums.ApiKeyRoles;
import com.processing.gateway.models.ApiKey;
import com.processing.gateway.storage.ApiKeyStorage;
import com.processing.gateway.utils.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    @Value("${gateway.api-keys.ttl-min:60}")
    private int ttl;

    private final ApiKeyStorage apiKeyStorage;
    private final ApiKeyGenerator apiKeyGenerator;

    public ApiKey issueKey(ApiKeyRoles role) {
        var key = new ApiKey(
                apiKeyGenerator.generate(),
                role,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(ttl)
        );

        apiKeyStorage.add(key);

        return key;
    }
}
