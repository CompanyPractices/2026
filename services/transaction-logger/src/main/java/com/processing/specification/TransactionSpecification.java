package com.processing.specification;

import com.processing.model.Transaction;
import com.processing.model.Transaction_;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> filter(TransactionFilter filter) {
        return Specification
                .where(equals(Transaction_.PAN, filter.getPan()))
                .and(equals(Transaction_.STATUS, filter.getStatus()))
                .and(dateFrom(filter.getFrom()))
                .and(dateTo(filter.getTo()))
                .and(equals(Transaction_.MERCHANT_ID, filter.getMerchantId()))
                .and(equals(Transaction_.ISSUER_ID, filter.getIssuerId()))
                .and(equals(Transaction_.MCC, filter.getMcc()))
                .and(fromPagingKey(filter.getFromPagingKey()));
    }

    private static Specification<Transaction> equals(String field, String value) {
        return (root, query, criteriaBuilder) ->
                value == null ? null : criteriaBuilder.equal(root.get(field), value);
    }

    private static Specification<Transaction> dateFrom(Instant date) {
        return (root, query, criteriaBuilder) ->
                date == null ? null :
                        criteriaBuilder.greaterThanOrEqualTo(root.get(Transaction_.CREATED_AT), date);
    }

    private static Specification<Transaction> dateTo(Instant date) {
        return (root, query, criteriaBuilder) ->
                date == null ? null :
                        criteriaBuilder.lessThan(root.get(Transaction_.CREATED_AT), date);
    }

    private static Specification<Transaction> fromPagingKey(UUID fromPagingKey) {
        return (root, query, criteriaBuilder) ->
                fromPagingKey == null ? null :
                        criteriaBuilder.greaterThan(root.get(Transaction_.ID), fromPagingKey.toString());
    }
}
