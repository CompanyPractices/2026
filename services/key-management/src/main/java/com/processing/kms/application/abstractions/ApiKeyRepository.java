package com.processing.kms.application.abstractions;

import com.processing.kms.domain.models.ApiKey;

public interface ApiKeyRepository {
    void addOrUpdate(ApiKey key);
    ApiKey get(String key);
    ApiKey getByOwnerId(String ownerId);
}
