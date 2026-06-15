package com.processing.gateway.dto;

import com.processing.gateway.enums.ApiKeyRoles;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        String key,
        ApiKeyRoles role,
        LocalDateTime issuedAt,
        LocalDateTime expiresBy
) {}
