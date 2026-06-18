package com.processing.cardmanagement.utils;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardEntity;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public final class CardManagementTestUtils {

    private static final Faker faker = new Faker();

    public static Card generateActiveCard() {
        return generateCard(faker, CardStatus.ACTIVE);
    }

    public static Card generateCard(Faker faker, CardStatus status) {
        var bin = faker.number().digits(16);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new Card(
            UUID.randomUUID(),
            bin + faker.number().digits(10),
            bin,
            faker.name().fullName().toUpperCase(Locale.ROOT),
            YearMonth.from(faker.timeAndDate().future().atOffset(ZoneOffset.UTC)),
            status,
            faker.number().digits(3),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            faker.lorem().characters(6)
        );
    }

    public static CardEntity generateActiveCardEntity() {
        return generateCardEntity(faker, "ACTIVE");
    }

    public static CardEntity generateCardEntity(Faker faker, String status) {
        var bin = faker.number().digits(16);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new CardEntity(
            UUID.randomUUID(),
            bin + faker.number().digits(10),
            bin,
            faker.name().fullName().toUpperCase(Locale.ROOT),
            formatDateToStrExpiryDate(faker.timeAndDate().future()),
            status,
            faker.number().digits(3),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            faker.lorem().characters(6),
            Instant.now()
        );
    }

    public static CardModel generateActiveCardModel() {
        return generateCardModel(faker, CardModelStatus.ACTIVE);
    }

    public static CardModel generateCardModel(Faker faker, CardModelStatus status) {
        var bin = faker.number().digits(16);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new CardModel(
            UUID.randomUUID(),
            bin + faker.number().digits(10),
            bin,
            faker.name().fullName().toUpperCase(Locale.ROOT),
            YearMonth.from(faker.timeAndDate().future().atOffset(ZoneOffset.UTC)),
            status,
            faker.number().digits(3),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            faker.lorem().characters(6),
            Instant.now()
        );
    }

    public static String formatDateToStrExpiryDate(Instant instant) {
        return formatDateToStrExpiryDate(YearMonth.from(instant.atOffset(ZoneOffset.UTC)));
    }

    public static String formatDateToStrExpiryDate(YearMonth date) {
        return String.format("%02d%02d", date.getMonthValue(), date.getYear() % 100);
    }

    public static java.time.YearMonth strExpiryDateToYearMonth(String strExpiryDate) {
        if (strExpiryDate == null || strExpiryDate.length() != 4) {
            throw new IllegalArgumentException("Invalid expiry date format. Expected MMyy");
        }

        int month = Integer.parseInt(strExpiryDate.substring(0, 2));
        int shortYear = Integer.parseInt(strExpiryDate.substring(2, 4));
        int currentCentury = (LocalDate.now().getYear() / 100) * 100;
        int fullYear = currentCentury + shortYear;

        return YearMonth.of(fullYear, month);
    }
}
