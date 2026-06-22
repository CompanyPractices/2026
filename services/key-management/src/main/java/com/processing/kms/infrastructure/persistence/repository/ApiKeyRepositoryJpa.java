package com.processing.kms.infrastructure.persistence.repository;

import com.processing.kms.application.abstractions.ApiKeyRepository;
import com.processing.kms.domain.models.ApiKey;
import com.processing.kms.infrastructure.persistence.entities.ApiKeyEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryJpa implements ApiKeyRepository {
    private final ApiKeyJpaRepository jpaRepository;
    private final ModelMapper modelMapper;

    @Override
    public void addOrUpdate(ApiKey key) {
        ApiKeyEntity entity = modelMapper.map(key, ApiKeyEntity.class);
        jpaRepository.save(entity);
    }

    @Override
    public ApiKey get(String key) {
        ApiKeyEntity entity = jpaRepository.getReferenceById(key);
        return modelMapper.map(entity, ApiKey.class);
    }

    @Override
    public ApiKey getByOwnerId(String ownerId) {
        ApiKeyEntity entity = jpaRepository.getByOwnerId(ownerId);

        if (entity == null) {
            return null;
        }

        return modelMapper.map(entity, ApiKey.class);
    }
}
