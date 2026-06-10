package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CardCriteriaBuilderJpaRepositoryImpl
    implements CardCriteriaBuilderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CardEntity> findCards(
        long limit,
        long offset,
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(CardEntity.class);
        var cardEntity = criteriaQuery.from(CardEntity.class);
        var predicates = createFiltersPredicate(
            criteriaBuilder,
            cardEntity,
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );

        criteriaQuery
            .select(cardEntity)
            .where(predicates.toArray(Predicate[]::new));
        return entityManager
            .createQuery(criteriaQuery)
            .setMaxResults((int) limit)
            .setFirstResult((int) offset)
            .getResultList();
    }

    @Override
    public long countCards(
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(Long.class);
        var cardEntity = criteriaQuery.from(CardEntity.class);
        var predicates = createFiltersPredicate(
            criteriaBuilder,
            cardEntity,
            status,
            bin,
            issuerId,
            startDate,
            endDate
        );

        criteriaQuery
            .select(criteriaBuilder.count(cardEntity))
            .where(criteriaBuilder.and(predicates.toArray(Predicate[]::new)));
        return entityManager
            .createQuery(criteriaQuery)
            .getSingleResult();
    }

    private List<Predicate> createFiltersPredicate(
        CriteriaBuilder criteriaBuilder,
        Root<CardEntity> root,
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable LocalDateTime startDate,
        @Nullable LocalDateTime endDate
    ) {
        var predicates = new ArrayList<Predicate>();
        if (status != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), status));
        }
        if (bin != null) {
            predicates.add(criteriaBuilder.equal(root.get("bin"), bin));
        }
        if (issuerId != null) {
            predicates.add(criteriaBuilder.equal(root.get("issuerId"), issuerId));
        }
        if (startDate != null) {
            predicates.add(criteriaBuilder.equal(root.get("startDate"), startDate));
        }
        if (endDate != null) {
            predicates.add(criteriaBuilder.equal(root.get("endDate"), endDate));
        }
        return predicates;
    }
}
