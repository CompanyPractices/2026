package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetry);

    List<OutboxEventEntity> findByStatus(EventStatus status);

    long countByStatus(EventStatus status);
}
