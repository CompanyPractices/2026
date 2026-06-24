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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceIssueTest {

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
        // Mocking KmsProperties behavior for initialization
        KmsProperties.ApiClient clientMock = mock(KmsProperties.ApiClient.class);
        when(clientMock.getType()).thenReturn("SERVICE_APP");
        when(clientMock.getRole()).thenReturn(ApiKeyRole.ADMIN); // Assuming ApiKeyRole enum exists

        when(kmsProperties.getClients()).thenReturn(List.of(clientMock));
        when(kmsProperties.getTtlMin()).thenReturn(60);

        apiKeyService.initialize();
    }

    @Test
    void issueKey_NewClient_Success() {
        String clientId = "client-123";
        String rawKey = "generated-raw-key";
        String hashedKey = "hashed-key";

        when(apiKeyRepository.getByOwnerId(clientId)).thenReturn(null);
        when(apiKeyGenerator.generate()).thenReturn(rawKey);

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(rawKey)).thenReturn(hashedKey);

            Result<ApiKey, KeyError> result = apiKeyService.issueKey("SERVICE_APP", clientId);

            assertInstanceOf(Result.Success.class, result);
            ApiKey issuedKey = ((Result.Success<ApiKey, KeyError>) result).value(); // Assuming value() or getValue()

            assertEquals(clientId, issuedKey.getOwnerId());
            assertEquals(ApiKeyRole.ADMIN, issuedKey.getRole());
            assertEquals(rawKey, issuedKey.getKey());
            assertFalse(issuedKey.getIsExpired());

            verify(apiKeyRepository).addOrUpdate(any(ApiKey.class));
        }
    }

    @Test
    void issueKey_ExistingActiveKey_ExpiresOldAndIssuesNew() {
        String clientId = "client-456";
        ApiKey existingKey = new ApiKey("old-hash", clientId, ApiKeyRole.ADMIN, Instant.now(), Instant.now().plusSeconds(3600), false);

        when(apiKeyRepository.getByOwnerId(clientId)).thenReturn(existingKey);
        when(apiKeyGenerator.generate()).thenReturn("new-raw-key");

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(anyString())).thenReturn("new-hashed-key");

            apiKeyService.issueKey("SERVICE_APP", clientId);

            // Verify old key was expired and updated
            assertTrue(existingKey.getIsExpired());
            verify(apiKeyRepository).addOrUpdate(existingKey);

            // Verify new key was created and saved (addOrUpdate called twice overall)
            verify(apiKeyRepository, times(2)).addOrUpdate(any(ApiKey.class));
        }
    }

    @Test
    void issueKey_UnknownClientType_IssuesWithNullRole() {
        String clientId = "client-789";
        when(apiKeyRepository.getByOwnerId(clientId)).thenReturn(null);
        when(apiKeyGenerator.generate()).thenReturn("raw");

        try (MockedStatic<ApiKeyHasher> hasher = mockStatic(ApiKeyHasher.class)) {
            hasher.when(() -> ApiKeyHasher.hash(anyString())).thenReturn("hash");

            // Providing an unmapped client type
            Result<ApiKey, KeyError> result = apiKeyService.issueKey("UNKNOWN_APP", clientId);

            assertInstanceOf(Result.Success.class, result);
            ApiKey issuedKey = ((Result.Success<ApiKey, KeyError>) result).value();

            // Role should be null since it wasn't in the initialized map
            assertNull(issuedKey.getRole());
        }
    }
}
