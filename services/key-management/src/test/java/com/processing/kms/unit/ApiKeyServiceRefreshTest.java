package com.processing.kms.unit;

import com.processing.common.result.Result;
import com.processing.kms.application.abstractions.ApiKeyRepository;
import com.processing.kms.application.service.ApiKeyServiceImpl;
import com.processing.kms.application.service.properties.KmsProperties;
import com.processing.kms.application.service.utils.ApiKeyGenerator;
import com.processing.kms.application.service.utils.ApiKeyHasher;
import com.processing.kms.domain.contracts.errors.KeyError;
import com.processing.kms.domain.models.ApiKey;
import com.processing.kms.domain.models.ApiKeyRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceRefreshTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private ApiKeyGenerator apiKeyGenerator;
    @Mock
    private KmsProperties kmsProperties;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    @BeforeEach
    void setUp() {
        when(kmsProperties.getClients()).thenReturn(Collections.emptyList());
        when(kmsProperties.getTtlMin()).thenReturn(120);
        apiKeyService.initialize();
    }

    @Test
    void refreshKey_ValidKeyAndOwner_ExpiresOldAndReturnsNew() {
        String ownerId = "owner-99";
        String oldRawKey = "old-raw";
        String oldHashedKey = "old-hashed";
        String newRawKey = "new-raw";
        String newHashedKey = "new-hashed";

        ApiKey oldApiKey = new ApiKey(oldHashedKey, ownerId, ApiKeyRole.READ, Instant.now(), Instant.now().plusSeconds(1000), false);

        when(apiKeyRepository.get(oldHashedKey)).thenReturn(oldApiKey);
        when(apiKeyGenerator.generate()).thenReturn(newRawKey);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            // Setup hasher behavior for both old validation and new generation
            hasher.when(() -> ApiKeyHasher.hash(oldRawKey)).thenReturn(oldHashedKey);
            hasher.when(() -> ApiKeyHasher.hash(newRawKey)).thenReturn(newHashedKey);

            Result<ApiKey, KeyError> result = apiKeyService.refreshKey(ownerId, oldRawKey);

            assertInstanceOf(Result.Success.class, result);
            ApiKey newKey = ((Result.Success<ApiKey, KeyError>) result).value();

            // Verify old key is disabled
            assertTrue(oldApiKey.getIsExpired());
            verify(apiKeyRepository).addOrUpdate(oldApiKey);

            // Verify new key properties
            assertEquals(ownerId, newKey.getOwnerId());
            assertEquals(newRawKey, newKey.getKey());
            verify(apiKeyRepository).addOrUpdate(newKey);
        }
    }

    @Test
    void refreshKey_KeyNotFound_ReturnsFailureNotFound() {
        String ownerId = "owner-99";
        String oldRawKey = "missing-key";

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(oldRawKey)).thenReturn("missing-hashed");
            when(apiKeyRepository.get("missing-hashed")).thenReturn(null);

            Result<ApiKey, KeyError> result = apiKeyService.refreshKey(ownerId, oldRawKey);

            assertInstanceOf(Result.Failure.class, result);
            assertInstanceOf(KeyError.NotFound.class, ((Result.Failure<ApiKey, KeyError>) result).error());
            verify(apiKeyRepository, never()).addOrUpdate(any());
        }
    }

    @Test
    void refreshKey_OwnerIdMismatch_ReturnsFailureOwnerMismatch() {
        String requestedOwnerId = "malicious-actor";
        String actualOwnerId = "legit-owner";
        String oldRawKey = "stolen-key";
        String oldHashedKey = "stolen-hashed";

        ApiKey stolenKeyInfo = new ApiKey(oldHashedKey, actualOwnerId, ApiKeyRole.ADMIN, Instant.now(), Instant.now().plusSeconds(1000), false);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(oldRawKey)).thenReturn(oldHashedKey);
            when(apiKeyRepository.get(oldHashedKey)).thenReturn(stolenKeyInfo);

            Result<ApiKey, KeyError> result = apiKeyService.refreshKey(requestedOwnerId, oldRawKey);

            assertInstanceOf(Result.Failure.class, result);
            assertInstanceOf(KeyError.OwnerIdMismatch.class, ((Result.Failure<ApiKey, KeyError>) result).error());

            // Ensure no modifications occurred
            assertFalse(stolenKeyInfo.getIsExpired());
            verify(apiKeyRepository, never()).addOrUpdate(any());
        }
    }
}
