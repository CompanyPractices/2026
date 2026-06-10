package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardJpaRepository extends JpaRepository<CardEntity, UUID>, CardCriteriaBuilderRepository {

    Optional<CardEntity> findByPan(String pan);
}
