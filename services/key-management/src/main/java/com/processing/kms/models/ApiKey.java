package com.processing.kms.models;

import java.time.Instant;

public record ApiKey(
        String key,
        String ownerId,
        ApiKeyRoles role,
        Instant issuedAt,
        Instant expiresBy
) {}
