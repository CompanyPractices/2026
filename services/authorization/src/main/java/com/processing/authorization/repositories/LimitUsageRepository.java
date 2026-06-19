package com.processing.authorization.repositories;

import com.processing.authorization.entities.LimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Репозиторий для таблицы limit_usage
 *
 * @see JpaRepository
 */
@Repository
public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {
        int deleteByUsageDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Modifying
        //@Transactional
        @Query(value =
                "INSERT INTO limit_usage (id, pan, usage_date, daily_amount, monthly_amount) "
                + "SELECT gen_random_uuid(), :pan, :date, :amount, "
                + "       COALESCE((SELECT lu.monthly_amount "
                + "                 FROM limit_usage lu "
                + "                 WHERE lu.pan = :pan "
                + "                 AND lu.usage_date = (SELECT MAX(lu2.usage_date) "
                + "                                      FROM limit_usage lu2 "
                + "                                      WHERE lu2.pan = :pan "
                + "                                      AND DATE_TRUNC('month', lu2.usage_date) = DATE_TRUNC('month', CAST(:date AS date)))), 0) + :amount "
                + "WHERE :amount <= :dailyLimit "
                + "AND COALESCE((SELECT lu.monthly_amount "
                + "              FROM limit_usage lu "
                + "              WHERE lu.pan = :pan "
                + "              AND lu.usage_date = (SELECT MAX(lu2.usage_date) "
                + "                                   FROM limit_usage lu2 "
                + "                                   WHERE lu2.pan = :pan "
                + "                                   AND DATE_TRUNC('month', lu2.usage_date) = DATE_TRUNC('month', CAST(:date AS date)))), 0) + :amount <= :monthlyLimit "
                + "ON CONFLICT (pan, usage_date) DO UPDATE SET "
                + "daily_amount = limit_usage.daily_amount + :amount, "
                + "monthly_amount = limit_usage.monthly_amount + :amount "
                + "WHERE limit_usage.daily_amount + :amount <= :dailyLimit "
                + "AND limit_usage.monthly_amount + :amount <= :monthlyLimit", nativeQuery = true
        )
        int upsertLimitUsage(@Param("pan") String pan,
                        @Param("date") LocalDate date,
                        @Param("amount") BigDecimal amount,
                        @Param("dailyLimit") BigDecimal dailyLimit,
                        @Param("monthlyLimit") BigDecimal monthlyLimit);

}
