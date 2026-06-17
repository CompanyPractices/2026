package com.processing.kms.controller;

import com.processing.common.result.Result;
import com.processing.kms.dto.*;
import com.processing.kms.models.ApiKeyRoles;
import com.processing.kms.models.ApiKey;
import com.processing.kms.errors.KeyError;
import com.processing.kms.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    @PostMapping("/issue")
    public ResponseEntity<ApiKey> issueApiKey(@RequestBody IssueRequest request) {
        ApiKey apiKey = apiKeyService.issueKey(
                request.clientId(),
                ApiKeyRoles.valueOf(request.role().toUpperCase()));

        return ResponseEntity.ok(apiKey);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateApiKey(@RequestBody String key) {
        Result<ApiKeyRoles, KeyError> result = apiKeyService.validateKey(key);

        return switch (result) {
            case Result.Success(ApiKeyRoles value) ->
                    ResponseEntity.ok(new ValidationResponse(
                            ValidationOutcome.VALID,
                            value,
                            null));
            case Result.Failure(KeyError.NotFound error) ->
                    ResponseEntity.ok(new ValidationResponse(
                            ValidationOutcome.INVALID,
                            null,
                            error.message()));
            case Result.Failure(KeyError.Expired error) ->
                    ResponseEntity.ok(new ValidationResponse(
                            ValidationOutcome.INVALID,
                            null,
                            error.message()));
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refreshApiKey(@RequestBody RefreshRequest request) {
        Result<ApiKey, KeyError> result = apiKeyService
                .refreshKey(request.clientId(), request.oldKey());

        return switch (result) {
            case Result.Success(ApiKey value) ->
                    ResponseEntity.ok(new RefreshResponse(
                            RefreshOutcome.SUCCESS,
                            value.key(),
                            null));
            case Result.Failure(KeyError.OwnerIdMismatch error) ->
                    ResponseEntity.ok(new RefreshResponse(
                            RefreshOutcome.FAILURE,
                            null,
                            error.message()));
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };
    }
}
