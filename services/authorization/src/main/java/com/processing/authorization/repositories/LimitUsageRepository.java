package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
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
            @Param("pan") String pan,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // @Modifying
    // @Query(value = """
    // INSERT INTO limit_usage (id, pan, usage_date, daily_amount, monthly_amount)
    // VALUES (gen_random_uuid(), :pan, :date, :daily_amount, :monthly_amount)
    // ON CONFLICT (pan, usage_date)
    // DO UPDATE SET
    // daily_amount = limit_usage.daily_amount + :amount,
    // monthly_amount = limit_usage.monthly_amount + :amount,
    // updated_at = NOW()
    // WHERE
    // limit_usage.daily_amount + :amount <= :dailyLimit
    // AND limit_usage.monthly_amount + :amount <= :monthlyLimit
    // """, nativeQuery = true)
    // // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // int upsertWithLimitCheck(
    // @Param("pan") String pan,
    // @Param("date") LocalDate date,
    // @Param("amount") BigDecimal amount,
    // @Param("dailyLimit") BigDecimal dailyLimit,
    // @Param("monthlyLimit") BigDecimal monthlyLimit);

    int deleteByUsageDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
