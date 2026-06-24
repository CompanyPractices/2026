package com.processing.kms.unit;

import com.processing.common.result.Result;
import com.processing.kms.application.abstractions.ApiKeyRepository;
import com.processing.kms.application.service.ApiKeyServiceImpl;
import com.processing.kms.application.service.utils.ApiKeyHasher;
import com.processing.kms.domain.contracts.errors.KeyError;
import com.processing.kms.domain.models.ApiKey;
import com.processing.kms.domain.models.ApiKeyRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceValidateTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    @Test
    void validateKey_ValidAndActive_ReturnsSuccessWithRole() {
        String rawKey = "my-secret-key";
        String hashedKey = "hashed-secret-key";
        ApiKey activeKey = new ApiKey(hashedKey, "owner-1", ApiKeyRole.READ, Instant.now(), Instant.now().plusSeconds(3600), false);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(rawKey)).thenReturn(hashedKey);
            when(apiKeyRepository.get(hashedKey)).thenReturn(activeKey);

            Result<ApiKeyRole, KeyError> result = apiKeyService.validateKey(rawKey);

            assertTrue(result instanceof Result.Success);
            assertEquals(ApiKeyRole.READ, ((Result.Success<ApiKeyRole, KeyError>) result).value());
            verify(apiKeyRepository, never()).addOrUpdate(any());
        }
    }

    @Test
    void validateKey_KeyNotFound_ReturnsFailureNotFound() {
        String rawKey = "unknown-key";

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(rawKey)).thenReturn("hashed-unknown");
            when(apiKeyRepository.get("hashed-unknown")).thenReturn(null);

            Result<ApiKeyRole, KeyError> result = apiKeyService.validateKey(rawKey);

            assertTrue(result instanceof Result.Failure);
            assertTrue(((Result.Failure<ApiKeyRole, KeyError>) result).error() instanceof KeyError.NotFound);
        }
    }

    @Test
    void validateKey_FlaggedAsExpired_ReturnsFailureExpired() {
        String rawKey = "expired-key";
        String hashedKey = "hashed-expired";
        // Key with future expiration date, but explicitly flagged as expired
        ApiKey expiredKey = new ApiKey(hashedKey, "owner-1", ApiKeyRole.READ, Instant.now(), Instant.now().plusSeconds(3600), true);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(rawKey)).thenReturn(hashedKey);
            when(apiKeyRepository.get(hashedKey)).thenReturn(expiredKey);

            Result<ApiKeyRole, KeyError> result = apiKeyService.validateKey(rawKey);

            assertTrue(result instanceof Result.Failure);
            assertTrue(((Result.Failure<ApiKeyRole, KeyError>) result).error() instanceof KeyError.Expired);
        }
    }

    @Test
    void validateKey_PastExpiryDate_SetsExpiredAndReturnsFailure() {
        String rawKey = "timed-out-key";
        String hashedKey = "hashed-timed-out";
        // Key not flagged as expired, but expiration timestamp is in the past
        ApiKey timedOutKey = new ApiKey(hashedKey, "owner-1", ApiKeyRole.READ, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), false);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(rawKey)).thenReturn(hashedKey);
            when(apiKeyRepository.get(hashedKey)).thenReturn(timedOutKey);

            Result<ApiKeyRole, KeyError> result = apiKeyService.validateKey(rawKey);

            assertTrue(result instanceof Result.Failure);
            assertTrue(((Result.Failure<ApiKeyRole, KeyError>) result).error() instanceof KeyError.Expired);

            // Validate that the service auto-corrected the flag and saved to DB
            assertTrue(timedOutKey.getIsExpired());
            verify(apiKeyRepository).addOrUpdate(timedOutKey);
        }
    }
}
