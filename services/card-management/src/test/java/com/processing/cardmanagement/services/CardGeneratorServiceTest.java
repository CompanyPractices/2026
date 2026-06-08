package com.processing.cardmanagement.services;

import com.processing.cardmanagement.options.CardGeneratorOptions;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.GeneratedCardDto;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CardGeneratorServiceTest {

    private final Faker faker = new Faker();

    @Mock
    private CardService cardService;

    @Mock
    private CardGeneratorOptions generatorOptions;

    @InjectMocks
    private CardGeneratorService cardGeneratorService;

    @BeforeEach
    void setUp() {
        when(generatorOptions.minBalance()).thenReturn(1_000_000);
        when(generatorOptions.maxBalance()).thenReturn(50_000_000);
        when(generatorOptions.minDailyLimit()).thenReturn(5_000_000);
        when(generatorOptions.maxDailyLimit()).thenReturn(30_000_000);
        when(generatorOptions.currencyCode()).thenReturn("643");
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
            List<GeneratedCardDto> dtos = inv.getArgument(0);
            return dtos.stream().map(dto -> new CardModel(
                    UUID.randomUUID(),
                    faker.numerify("################"),
                    dto.bin(),
                    faker.name().fullName().toUpperCase(),
                    "0629",
                    dto.status().name(),
                    "643",
                    dto.dailyLimit(),
                    dto.monthlyLimit(),
                    dto.balance(),
                    "ZZZZZZ",
                    LocalDateTime.now()
            )).toList();
        });

        List<CardModel> result = cardGeneratorService.generate(count, bins);

        assertEquals(count, result.size());
    }

    @Test
    void generateShouldDistributeEvenlyAcrossBin() {
        int count = 100;
        List<String> bins = List.of("400000", "400001", "400002", "400003", "400004");

        when(cardService.createCards(anyList())).thenAnswer(inv -> {
            List<GeneratedCardDto> dtos = inv.getArgument(0);
            return dtos.stream().map(dto -> new CardModel(
                    UUID.randomUUID(),
                    faker.numerify("################"),
                    dto.bin(),
                    faker.name().fullName().toUpperCase(),
                    "0629",
                    dto.status().name(),
                    "643",
                    dto.dailyLimit(),
                    dto.monthlyLimit(),
                    dto.balance(),
                    "ZZZZZZ",
                    LocalDateTime.now()
            )).toList();
        });

        List<CardModel> result = cardGeneratorService.generate(count, bins);

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
}
