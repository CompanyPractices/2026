package com.processing.gateway.storage;

import com.processing.gateway.models.ApiKey;
import com.processing.gateway.models.ApiKeyRoles;
import com.processing.gateway.properties.ApiKeysProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ApiKeyStorageInMemory implements ApiKeyStorage {
    private final ApiKeysProperties apiKeysProperties;
    private final Map<String, ApiKey> apiKeyMap;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Api key storage initialized");

        apiKeysProperties.getKeys().forEach((key, role) ->
                apiKeyMap.putIfAbsent(key, new ApiKey(
                    key,
                    ApiKeyRoles.valueOf(role),
                    Instant.MIN,
                    Instant.MAX
        )));
    }

//    @Value("${api-keys.master-key}")
//    private String masterKey;
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void initialize() {
//        log.info("Api key storage initialized");
//        apiKeyMap.putIfAbsent(masterKey, new ApiKey(
//                masterKey,
//                ApiKeyRoles.ADMIN,
//                Instant.MIN,
//                Instant.MAX
//        ));
//    }

    @Override
    public void add(ApiKey apiKey) {
        apiKeyMap.putIfAbsent(apiKey.key(), apiKey);
    }

    @Override
    public ApiKey get(String key) {
        return apiKeyMap.get(key);
    }
}
