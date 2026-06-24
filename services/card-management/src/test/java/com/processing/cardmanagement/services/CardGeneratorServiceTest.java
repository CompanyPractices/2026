package com.processing.cardmanagement.services;

import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.options.CardGeneratorOptions;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CardGeneratorServiceTest {

    private final Faker faker = new Faker();

    @Mock
    private CardService cardService;

    @Mock
    private CardEventNotifier eventNotifier;

    private final CardGeneratorOptions generatorOptions = new CardGeneratorOptions(
        BigDecimal.valueOf(1_000_000),
        BigDecimal.valueOf(50_000_000),
        BigDecimal.valueOf(5_000_000),
        BigDecimal.valueOf(30_000_000),
        "643",
        100
    );

    private CardGeneratorService cardGeneratorService;

    @BeforeEach
    void setUp() {
        cardGeneratorService = new CardGeneratorService(
            cardService,
            generatorOptions,
            eventNotifier
        );
    }

    @Test
    void generateShouldReturnCorrectCount() {
        int count = 100;
        List<String> bins = List.of(
            faker.numerify("######"),
            faker.numerify("######"),
            faker.numerify("######"),
            faker.numerify("######"),
            faker.numerify("######"));

        when(cardService.createCards(anyList())).thenAnswer(inv -> {
            List<CardDraft> dtos = inv.getArgument(0);
            return dtos.stream().map(dto -> new Card(
                UUID.randomUUID(),
                faker.numerify("################"),
                dto.bin(),
                faker.name().fullName().toUpperCase(),
                YearMonth.now().plusYears(3),
                dto.status(),
                "643",
                dto.dailyLimit(),
                dto.monthlyLimit(),
                dto.initialBalance(),
                "ZZZZZZ",
                Instant.now()
            )).toList();
        });

        List<Card> result = cardGeneratorService.generate(count, bins);

        assertEquals(count, result.size());
    }

    @Test
    void generateShouldDistributeEvenlyAcrossBin() {
        int count = 100;
        List<String> bins = List.of("400000", "400001", "400002", "400003", "400004");

        when(cardService.createCards(anyList())).thenAnswer(inv -> {
            List<CardDraft> dtos = inv.getArgument(0);
            return dtos.stream().map(dto -> new Card(
                UUID.randomUUID(),
                faker.numerify("################"),
                dto.bin(),
                faker.name().fullName().toUpperCase(),
                YearMonth.now().plusYears(3),
                dto.status(),
                "643",
                dto.dailyLimit(),
                dto.monthlyLimit(),
                dto.initialBalance(),
                "ZZZZZZ",
                Instant.now()
            )).toList();
        });

        List<Card> result = cardGeneratorService.generate(count, bins);

        long bin1count = result.stream().filter(c -> c.bin().equals("400000")).count();
        long bin2count = result.stream().filter(c -> c.bin().equals("400001")).count();
        long bin3count = result.stream().filter(c -> c.bin().equals("400002")).count();
        long bin4count = result.stream().filter(c -> c.bin().equals("400003")).count();
        long bin5count = result.stream().filter(c -> c.bin().equals("400004")).count();

        assertEquals(20, bin1count);
        assertEquals(20, bin2count);
        assertEquals(20, bin3count);
        assertEquals(20, bin4count);
        assertEquals(20, bin5count);
    }

    @Test
    void correctCountStatusesGeneration() {
        int count = 100;
        List<String> bins = List.of("400000", "400001", "400002", "400003", "400004");

        when(cardService.createCards(anyList())).thenAnswer(inv -> {
            List<CardDraft> dtos = inv.getArgument(0);
            return dtos.stream().map(dto -> new Card(
                UUID.randomUUID(),
                faker.numerify("################"),
                dto.bin(),
                faker.name().fullName().toUpperCase(),
                YearMonth.now().plusYears(3),
                dto.status(),
                "643",
                dto.dailyLimit(),
                dto.monthlyLimit(),
                dto.initialBalance(),
                "ZZZZZZ",
                Instant.now()
            )).toList();
        });

        List<Card> result = cardGeneratorService.generate(count, bins);

        long activeCount = result.stream().filter(c -> c.status() == CardStatus.ACTIVE).count();
        long inactiveCount = result.stream().filter(c -> c.status() == CardStatus.INACTIVE).count();
        long blockedCount = result.stream().filter(c -> c.status() == CardStatus.BLOCKED).count();

        assertEquals(95, activeCount);
        assertEquals(3, inactiveCount);
        assertEquals(2, blockedCount);
    }
}
