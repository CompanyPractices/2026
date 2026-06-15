package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для таблицы limit_usage
 *
 * @see JpaRepository
 */
@Repository
public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LimitUsage> findByPanAndUsageDate(String pan, LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LimitUsage> findTopByPanAndUsageDateBetweenOrderByUsageDateDesc(
        @Param("pan")String pan,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
