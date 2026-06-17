package com.processing.kms.repository;

import com.processing.kms.models.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryJpa implements ApiKeyRepository {
    private final ApiKeyJpaRepository jpaRepository;

    @Override
    public void addOrUpdate(ApiKey key) {
        jpaRepository.save(key);
    }

    @Override
    public ApiKey get(String key) {
        return jpaRepository.getReferenceById(key);
    }

    @Override
    public ApiKey getByOwnerId(String ownerId) {
        return jpaRepository.getByOwnerId(ownerId);
    }
}
