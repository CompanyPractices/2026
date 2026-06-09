package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<LimitUsage> findByPanAndUsageDate(String pan, LocalDate usageDate);
}
