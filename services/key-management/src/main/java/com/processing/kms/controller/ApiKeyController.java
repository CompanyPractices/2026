package com.processing.kms.controller;

import com.processing.common.dto.ErrorResponse;
import com.processing.kms.dto.ApiKeyRequest;
import com.processing.kms.dto.ValidationOutcome;
import com.processing.kms.dto.ValidationResponse;
import com.processing.kms.models.ApiKeyRoles;
import com.processing.kms.models.ApiKey;
import com.processing.kms.result.Result;
import com.processing.kms.result.errors.KeyError;
import com.processing.kms.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    @PostMapping("/issue")
    public ResponseEntity<ApiKey> issueApiKey(@RequestBody ApiKeyRequest request) {
        ApiKey apiKey = apiKeyService.issueKey(ApiKeyRoles.valueOf(request.role().toUpperCase()));
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
            case Result.Failure(KeyError.KeyNotFoundError error) ->
                    ResponseEntity.ok(new ValidationResponse(
                            ValidationOutcome.INVALID,
                            null,
                            "Key not found"));
            case Result.Failure(KeyError.KeyExpiredError error) ->
                    ResponseEntity.ok(new ValidationResponse(
                            ValidationOutcome.INVALID,
                            null,
                            "Key expired"));
        };
    }

//    @PostMapping("/refresh")
//    public ResponseEntity refreshApiKey() {
//
//    }
}
