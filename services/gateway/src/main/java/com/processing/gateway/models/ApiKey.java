package com.processing.gateway.models;

import java.time.Instant;

public record ApiKey(
        String key,
        ApiKeyRoles role,
        Instant issuedAt,
        Instant expiresBy
) {}
