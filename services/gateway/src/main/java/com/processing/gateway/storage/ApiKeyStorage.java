package com.processing.gateway.storage;

import com.processing.gateway.models.ApiKey;

public interface ApiKeyStorage {
    void add(ApiKey key);
    ApiKey get(String key);
}
