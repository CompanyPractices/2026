package com.processing.kms.infrastructure.persistence.entities;

import com.processing.kms.domain.models.ApiKeyRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "api_keys")
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKeyEntity {
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
