package com.processing.kms.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "api_keys")
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey {
    @Id
    private String key;

    @Column(name = "owner_id", unique = true)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApiKeyRole role;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_by")
    private Instant expiresBy;

    @Column(name = "is_expired")
    private Boolean isExpired;
}
