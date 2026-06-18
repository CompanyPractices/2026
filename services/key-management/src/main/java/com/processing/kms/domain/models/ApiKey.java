package com.processing.kms.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiKey {
    private String key;

    private String ownerId;

    private ApiKeyRole role;

    private Instant issuedAt;

    private Instant expiresBy;

    private Boolean isExpired;
}
