package com.processing.gateway.models;

import com.processing.gateway.enums.ApiKeyRoles;

import java.time.LocalDateTime;

public record ApiKey(
        String key,
        ApiKeyRoles role,
        LocalDateTime issuedAt,
        LocalDateTime expiresBy
) {}
