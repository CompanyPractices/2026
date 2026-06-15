package com.processing.gateway.controller;

import com.processing.gateway.dto.ApiKeyRequest;
import com.processing.gateway.enums.ApiKeyRoles;
import com.processing.gateway.models.ApiKey;
import com.processing.gateway.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKey> issueApiKey(@RequestBody ApiKeyRequest request) {
        ApiKey apiKey = apiKeyService.issueKey(ApiKeyRoles.valueOf(request.role().toUpperCase()));

        return ResponseEntity.ok(apiKey);
    }
}
