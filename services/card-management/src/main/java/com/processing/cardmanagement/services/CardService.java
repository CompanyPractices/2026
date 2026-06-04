package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.options.CardServiceOptions;
import com.processing.cardmanagement.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardServiceOptions options;
    private final PanGenerator panGenerator;

    public CardDto createCard(CreateCardRequest data) {
        var entity = cardRepository.save(
            new CardEntity(
                panGenerator.generatePan(data.bin()),
                data.bin(),
                data.cardholderName(),
                data.currencyCode(),
                CardEntity.Status.ACTIVE,
                data.dailyLimit(),
                data.monthlyLimit(),
                data.initialBalance(),
                options.issuerId()
            )
        );
        return CardDto.fromEntity(entity);
    }

    public List<CardDto> createCards(List<GeneratedCardDto> data) {
        var entities = data
            .stream()
            .map(dto -> new CardEntity(
                panGenerator.generatePan(dto.bin()),
                dto.bin(),
                dto.cardholderName(),
                dto.currencyCode(),
                dto.status(),
                dto.dailyLimit(),
                dto.monthlyLimit(),
                dto.balance(),
                options.issuerId()
            ))
            .toList();

        return cardRepository
            .saveAll(entities)
            .stream()
            .map(CardDto::fromEntity)
            .toList();
    }

    public CardDto getCard(String pan) {
        return CardDto.fromEntity(getCardEntity(pan));
    }

    public List<CardDto> getCards(GetCardsRequest data) {
        return cardRepository
            .findCards(
                data.status(),
                data.bin(),
                data.issuerId(),
                data.startDate(),
                data.endDate(),
                PageRequest.of(data.offset(), data.limit())
            )
            .stream()
            .map(CardDto::fromEntity)
            .toList();
    }

    public void patchCard(String pan, PatchCardRequest data) {
        var card = getCardEntity(pan);

        if (data.status() != null) {
            card.setStatus(data.status());
        }
        if (data.dailyLimit() != null) {
            card.setDailyLimit(data.dailyLimit());
        }
        if (data.monthlyLimit() != null) {
            card.setMonthlyLimit(data.monthlyLimit());
        }
        if (data.availableBalance() != null) {
            card.setAvailableBalance(data.availableBalance());
        }

        cardRepository.save(card);
    }

    public void deleteCard(String pan) {
        var card = getCardEntity(pan);
        card.delete();
        cardRepository.save(card);
    }

    public long countCards() {
        return cardRepository.count();
    }

    public void reserve(String pan, ReserveRequest data) {
        var card = getCardEntity(pan);
        card.reserve(data.amount());
        cardRepository.save(card);
    }

    private CardEntity getCardEntity(String pan) {
        return cardRepository
            .findByPan(pan)
            .orElseThrow(CardNotFoundException::new);
    }
}
