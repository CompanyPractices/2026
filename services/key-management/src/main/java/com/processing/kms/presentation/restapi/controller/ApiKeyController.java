package com.processing.kms.presentation.restapi.controller;

import com.processing.common.result.Result;
import com.processing.kms.domain.contracts.ApiKeyService;
import com.processing.kms.domain.models.ApiKeyRole;
import com.processing.kms.domain.models.ApiKey;
import com.processing.kms.domain.contracts.errors.KeyError;
import com.processing.kms.presentation.restapi.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    @PostMapping("/issue")
    public ResponseEntity<IssueResponse> issueApiKey(@RequestBody IssueRequest request) {
        Result<ApiKey, KeyError> result = apiKeyService.issueKey(request.clientType());

        return switch (result) {
            case Result.Success<ApiKey, KeyError>(ApiKey value) ->
                    ResponseEntity.ok(new IssueResponse(
                            true,
                            value,
                            null));
            case Result.Failure<ApiKey, KeyError>(KeyError.OwnerAlreadyHasKey error) ->
                    ResponseEntity.ok(new IssueResponse(
                            false,
                            null,
                            error.message()));
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateApiKey(@RequestBody ValidateRequest request) {
        Result<ApiKeyRole, KeyError> result = apiKeyService.validateKey(request.key());

        return switch (result) {
            case Result.Success(ApiKeyRole value) ->
                    ResponseEntity.ok(new ValidationResponse(
                            true,
                            value,
                            null));

            case Result.Failure(KeyError.NotFound error) ->
                    ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(new ValidationResponse(
                                false,
                                null,
                                error.message()));

            case Result.Failure(KeyError.Expired error) ->
                    ResponseEntity.ok(new ValidationResponse(
                                false,
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
                            true,
                            value.getKey(),
                            null));
            case Result.Failure(KeyError.NotFound error) ->
                    ResponseEntity.ok(new RefreshResponse(
                            false,
                            null,
                            error.message()));
            case Result.Failure(KeyError.OwnerIdMismatch error) ->
                    ResponseEntity.ok(new RefreshResponse(
                            false,
                            null,
                            error.message()));
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };
    }
}
