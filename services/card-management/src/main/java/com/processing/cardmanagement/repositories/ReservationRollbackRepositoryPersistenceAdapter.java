package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.mappers.ReservationRollbackPersistenceMapper;
import com.processing.cardmanagement.models.ReservationRollback;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReservationRollbackRepositoryPersistenceAdapter
    implements ReservationRollbackRepository {

    private ReservationRollbackPersistenceMapper mapper;
    private ReservationRollbackJpaRepository jpaRepository;

    @Transactional
    @Override
    public ReservationRollback save(ReservationRollback rollback) {
        return mapper.toDomain(
            jpaRepository.save(mapper.toEntity(rollback))
        );
    }
}
