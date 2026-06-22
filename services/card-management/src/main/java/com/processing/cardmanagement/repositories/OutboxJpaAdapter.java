package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.EventStatus;
import com.processing.cardmanagement.models.OutboxEventEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OutboxJpaAdapter implements OutboxRepository {

    private final OutboxEventJpaRepository jpaRepository;

    @Override
    public OutboxEventEntity save(OutboxEventEntity event) {
        return jpaRepository.save(event);
    }

    @Override
    public Optional<OutboxEventEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<OutboxEventEntity> findPending(int maxRetry) {
        return jpaRepository.findByStatusAndRetryCountLessThan(EventStatus.PENDING, maxRetry);
    }

    @Override
    public List<OutboxEventEntity> findFailed() {
        return jpaRepository.findByStatus(EventStatus.FAILED);
    }

    @Override
    public long countByStatus(EventStatus status) {
        return jpaRepository.countByStatus(status);
    }
}
