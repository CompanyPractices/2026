package com.processing.services;

import com.processing.models.CardEntity;
import com.processing.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CardGeneratorService {
    private final CardRepository cardRepository;
    private final PanGenerator panGenerator;

    private final Random random = new Random();

    @Value("${app.card-service.issuer-id}")
    private String issuerId;

    @Value("${app.card-service.currency-code}")
    private String currencyCode;

    @Value("${app.card-service.min-balance}")
    private long minBalance;

    @Value("${app.card-service.max-balance}")
    private long maxBalance;

    @Value("${app.card-service.min-daily-limit}")
    private long minDailyLimit;

    @Value("${app.card-service.max-daily-limit}")
    private long maxDailyLimit;

    private static final List<String> NAMES = List.of(
          "IVAN IVANOV", "PETR PETROV", "ANNA SMIRNOVA", "ELENA VOLKOVA", "DMITRY SOKOLOV"
    );

    public List<CardEntity> generate(int count, List<String> bins) {
        List<CardEntity> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bin = bins.get(i % bins.size());

            String cardholderName = NAMES.get(random.nextInt(NAMES.size()));
            long balance = random.nextLong(minBalance, maxBalance);
            long dailyLimit = random.nextLong(minDailyLimit, maxDailyLimit);
            long monthlyLimit = dailyLimit * 30;

            CardEntity card = new CardEntity(
                    panGenerator.generatePan(bin),
                    bin,
                    cardholderName,
                    currencyCode,
                    dailyLimit,
                    monthlyLimit,
                    balance,
                    issuerId
            );
            card.setStatus(generateStatus());
            cards.add(card);
        }
        return cardRepository.saveAll(cards);
    }

    private CardEntity.Status generateStatus() {
        int roll = random.nextInt(100);

        if (roll < 95) {
            return CardEntity.Status.ACTIVE;
        }

        if (roll < 98) {
            return CardEntity.Status.INACTIVE;
        }

        return CardEntity.Status.BLOCKED;
    }
}
