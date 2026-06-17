package com.processing.kms.repository;

import com.processing.kms.models.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKey, String> {
    ApiKey getByOwnerId(String ownerId);
}
