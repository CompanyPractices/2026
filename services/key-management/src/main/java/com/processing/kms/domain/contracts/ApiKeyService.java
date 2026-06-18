package com.processing.kms.domain.contracts;

import com.processing.common.result.Result;
import com.processing.kms.domain.models.ApiKey;
import com.processing.kms.domain.models.ApiKeyRole;
import com.processing.kms.domain.contracts.errors.KeyError;

public interface ApiKeyService {
    Result<ApiKey, KeyError> issueKey(String clientType);
    Result<ApiKeyRole, KeyError> validateKey(String key);
    Result<ApiKey, KeyError> refreshKey(String ownerId, String oldKey);
}
