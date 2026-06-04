package com.processing.specification;

import com.processing.model.Transaction;
import com.processing.model.Transaction_;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class TransactionSpecification {

    public static Specification<Transaction> filter(TransactionFilter filter) {
        return Specification
                .where(equals(Transaction_.PAN, filter.getPan()))
                .and(equals(Transaction_.STATUS, filter.getStatus()))
                .and(dateFrom(filter.getDateFrom()))
                .and(dateTo(filter.getDateTo()))
                .and(equals(Transaction_.MERCHANT_ID, filter.getMerchantId()))
                .and(equals(Transaction_.ISSUER_ID, filter.getIssuerId()))
                .and(equals(Transaction_.MCC, filter.getMcc()));
    }

    private static Specification<Transaction> equals(String field, String value) {
        return (root, query, criteriaBuilder) ->
                value == null ? null : criteriaBuilder.equal(root.get(field), value);
    }

    private static Specification<Transaction> dateFrom(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return null;
            }
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction_.TRANSMISSION_DATE_TIME), from);
        };
    }

    private static Specification<Transaction> dateTo(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return null;
            }
            Instant from = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return criteriaBuilder.lessThan(root.get(Transaction_.TRANSMISSION_DATE_TIME), from);
        };
    }
}
