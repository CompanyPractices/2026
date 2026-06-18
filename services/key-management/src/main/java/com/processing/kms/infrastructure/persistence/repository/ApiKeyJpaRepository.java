package com.processing.kms.infrastructure.persistence.repository;

import com.processing.kms.infrastructure.persistence.entities.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyEntity, String> {
    ApiKeyEntity getByOwnerId(String ownerId);
}
