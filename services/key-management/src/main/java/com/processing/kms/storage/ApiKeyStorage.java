package com.processing.kms.storage;

import com.processing.kms.models.ApiKey;

public interface ApiKeyStorage {
    void add(ApiKey key);
    ApiKey get(String key);
}
