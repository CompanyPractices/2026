package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {
    OutboxEventEntity save(OutboxEventEntity event);

    Optional<OutboxEventEntity> findById(UUID id);

    List<OutboxEventEntity> findPending(int maxRetry);

    List<OutboxEventEntity> findFailed();

    long countByStatus(EventStatus status);
}
