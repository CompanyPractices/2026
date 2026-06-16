package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.events.CardGeneratedEvent;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardGeneratorOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Сервис для генерации тестовых банковских карт
 * Карты равномерно распределяются по переданным BIN-префиксам
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardGeneratorService {

    private final CardService cardService;
    private final CardGeneratorOptions generatorOptions;
    private final CardEventNotifier eventNotifier;

    private final Faker faker = new Faker();
    private final Random random = new Random();

    /**
     * Генерирует указанное количество тестовых карт и сохраняет их в базе данных
     * Карты равномерно распределяются по BIN-ам
     * Распределение статусов: ACTIVE - 95%, INACTIVE - 3%, BLOCKED - 2%
     *
     * @param count количество карт для генерации
     * @param bins  список BIN-префиксов для распределения карт
     * @return список созданных карт
     */
    public List<Card> generate(int count, List<String> bins) {
        log.info("Generating {} cards for bins: {}", count, bins);
        List<CardDraft> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bin = bins.get(i % bins.size());

            String cardholderName = faker.name().fullName().toUpperCase();
            BigDecimal balance = BigDecimal.valueOf(
                random.nextLong(
                    generatorOptions.minBalance().longValue(),
                    generatorOptions.maxBalance().longValue()
                )
            );
            BigDecimal dailyLimit = BigDecimal.valueOf(
                random.nextLong(
                    generatorOptions.minDailyLimit().longValue(),
                    generatorOptions.maxDailyLimit().longValue()
                )
            );
            BigDecimal monthlyLimit = BigDecimal.valueOf(
                dailyLimit.longValue() * 30L
            );


            CardDraft card = new CardDraft(
                bin,
                cardholderName,
                generateStatus(),
                generatorOptions.currencyCode(),
                dailyLimit,
                monthlyLimit,
                balance
            );

            eventNotifier.onEvent(new CardGeneratedEvent(card.status()));
            cards.add(card);
        }
        log.info("Successfully generated {} cards", count);
        return cardService.createCards(cards);
    }

    private CardStatus generateStatus() {
        int roll = random.nextInt(100);

        if (roll < 95) {
            return CardStatus.ACTIVE;
        }

        if (roll < 98) {
            return CardStatus.INACTIVE;
        }

        return CardStatus.BLOCKED;
    }
}
