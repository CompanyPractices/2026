package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.CardNotFoundException;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.options.CardServiceOptions;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardServiceOptions options;
    private final PanGenerator panGenerator;

    public CardModel createCard(CreateCardRequest data) {
        var entity = cardRepository.save(
            new CardEntity(
                panGenerator.generatePan(data.bin()),
                data.bin(),
                data.cardholderName(),
                data.currencyCode(),
                CardStatus.ACTIVE,
                data.dailyLimit(),
                data.monthlyLimit(),
                data.initialBalance(),
                options.issuerId()
            )
        );
        return cardModelFromEntity(entity);
    }

    public List<CardModel> createCards(List<GeneratedCardDto> data) {
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
            .map(this::cardModelFromEntity)
            .toList();
    }

    public CardModel getCard(String pan) {
        return cardModelFromEntity(getCardEntity(pan));
    }

    public GetCardsResponse getCards(
        Integer limit,
        Integer offset,
        CardStatus status,
        String bin,
        String issuerId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        var cards = cardRepository
            .findCards(
                status,
                bin,
                issuerId,
                startDate,
                endDate,
                PageRequest.of(offset, limit)
            )
            .stream()
            .map(this::cardModelFromEntity)
            .toList();

        int total = cardRepository.countCards(status, bin, issuerId, startDate, endDate);

        return new GetCardsResponse(total, cards);
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
        cardRepository.delete(card);
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

    private CardModel cardModelFromEntity(CardEntity entity) {
        return new CardModel(
            entity.getId(),
            entity.getPan(),
            entity.getBin(),
            entity.getCardholderName(),
            entity.getStrExpiryDate(),
            entity.getStatus().name(),
            entity.getDailyLimit(),
            entity.getMonthlyLimit(),
            entity.getAvailableBalance(),
            entity.getIssuerId(),
            entity.getCreatedAt()
        );
    }
}
