package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByStatusAndRetryCountLessThan(String status, int maxRetry);

    List<OutboxEventEntity> findByStatus(String status);

    long countByStatus(String status);
}
