package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<LimitUsage> findByPanAndUsageDate(String pan, LocalDate usageDate);

    @Query(
            "SELECT COALESCE(SUM(lu.dailyAmount), 0) "
          + "FROM LimitUsage lu "
          + "WHERE lu.pan = :pan AND lu.usageDate BETWEEN :startDate AND :endDate"
    )
    Long sumMonthlyAmountByPanAndMonth(
            @Param("pan")String pan,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
