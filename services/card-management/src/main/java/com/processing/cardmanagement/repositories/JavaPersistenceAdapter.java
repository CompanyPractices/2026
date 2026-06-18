package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.mappers.CardPersistenceMapper;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class JavaPersistenceAdapter implements CardRepository {

    private final CardPersistenceMapper persistenceMapper;
    private final CardJpaRepository jpaRepository;

    @Override
    public Optional<Card> findByPan(String pan) {
        return jpaRepository
            .findByPan(pan)
            .map(persistenceMapper::toDomain);
    }

    @Override
    public List<Card> findCards(
        int limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    ) {
        return jpaRepository
            .findCards(
                limit,
                offset,
                status != null ? status.name() : null,
                bin,
                issuerId,
                startDate,
                endDate
            )
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public long countCardsFiltered(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    ) {
        return jpaRepository.countCards(
            status != null ? status.name() : null,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    public long countAllCards() {
        return jpaRepository.count();
    }

    @Override
    public Card save(Card card) {
        return persistenceMapper.toDomain(
            jpaRepository.save(persistenceMapper.toEntity(card))
        );
    }

    @Override
    public List<Card> saveAll(List<Card> card) {
        return jpaRepository.saveAll(card
                .stream()
                .map(persistenceMapper::toEntity)
                .toList()
            )
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public Card updateWithPessimisticLock(String pan, UnaryOperator<Card> businessLogic) {
        var cardEntity = jpaRepository
            .findWithPessimisticLockByPan(pan)
            .orElseThrow();

        var card = businessLogic.apply(persistenceMapper.toDomain(cardEntity));
        persistenceMapper.updateEntityFromDomain(card, cardEntity);
        return persistenceMapper.toDomain(jpaRepository.save(cardEntity));
    }
}
