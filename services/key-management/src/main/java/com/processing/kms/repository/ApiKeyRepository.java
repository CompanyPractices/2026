package com.processing.kms.repository;

import com.processing.kms.models.ApiKey;

public interface ApiKeyRepository {
    void addOrUpdate(ApiKey key);
    ApiKey get(String key);
    ApiKey getByOwnerId(String ownerId);
}
