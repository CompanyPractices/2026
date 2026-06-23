package com.processing.cardmanagement.utils;

import com.processing.cardmanagement.models.*;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class CardManagementTestUtils {

    private static final Faker faker = new Faker();

    public static Card generateActiveCard() {
        return generateCard(faker, generatePan(faker), CardStatus.ACTIVE);
    }

    public static Card generateActiveCardByPan(String pan) {
        return generateCard(faker, pan, CardStatus.ACTIVE);
    }

    public static CardStatus randomCardStatus() {
        return randomCardStatus(faker);
    }

    public static CardStatus randomCardStatus(Faker faker) {
        var statuses = Arrays
            .stream(CardStatus.values())
            .filter(status -> status != CardStatus.DELETED)
            .toArray(CardStatus[]::new);
        return faker.options().option(statuses);
    }

    public static CardDraft generateCardDraft() {
        return generateCardDraft(faker);
    }

    public static CardDraft generateCardDraft(Faker faker) {
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new CardDraft(
            generateBin(faker),
            generateCardholderName(faker),
            randomCardStatus(faker),
            generateCurrencyCode(faker),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 15_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(1, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000))
        );
    }

    public static Card generateCard(String pan, CardStatus status) {
        return generateCard(faker, pan, status);
    }

    public static Card generateCard(Faker faker, String pan, CardStatus status) {
        var bin = pan.substring(0, 6);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new Card(
            UUID.randomUUID(),
            generatePan(faker, bin),
            bin,
            generateCardholderName(),
            YearMonth.from(faker.timeAndDate().future().atOffset(ZoneOffset.UTC)),
            status,
            generateCurrencyCode(faker),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            generateIssuerId(faker)
        );
    }

    public static CardEntity generateActiveCardEntity() {
        return generateCardEntity(faker, "ACTIVE");
    }

    public static CardEntity generateCardEntity(Faker faker, String status) {
        var bin = generateBin(faker);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new CardEntity(
            UUID.randomUUID(),
            generatePan(faker, bin),
            bin,
            generateCardholderName(),
            formatDateToStrExpiryDate(faker.timeAndDate().future()),
            status,
            generateCurrencyCode(faker),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            generateIssuerId(faker),
            Instant.now()
        );
    }

    public static CardModel generateActiveCardModel() {
        return generateCardModel(faker, CardModelStatus.ACTIVE);
    }

    public static CardModel generateCardModel(Faker faker, CardModelStatus status) {
        var bin = generateBin(faker);
        var dailyLimit = faker.number().numberBetween(1, 15_000_000);
        return new CardModel(
            UUID.randomUUID(),
            generatePan(faker, bin),
            bin,
            generateCardholderName(),
            YearMonth.from(faker.timeAndDate().future().atOffset(ZoneOffset.UTC)),
            status,
            generateCurrencyCode(faker),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000)),
            generateIssuerId(faker),
            Instant.now()
        );
    }

    public static Reservation generateReservation() {
        return generateReservation(faker, BigDecimal.valueOf(faker.number().numberBetween(1, 1_000_000)));
    }

    public static ReservationEntity generateReservationEntity() {
        return generateReservationEntity(faker, BigDecimal.valueOf(faker.number().numberBetween(1, 1_000_000)));
    }

    public static Reservation generateReservation(Faker faker, BigDecimal amount) {
        return new Reservation(
            generatePan(faker),
            amount,
            generateRrn(faker)
        );
    }

    public static ReservationEntity generateReservationEntity(Faker faker, BigDecimal amount) {
        return new ReservationEntity(
            UUID.randomUUID(),
            generatePan(faker),
            amount,
            generateRrn(faker),
            faker.options().option(ReservationStatus.class).name(),
            Instant.now(),
            Instant.now()
        );
    }

    public static ReservationRollback generateReservationRollback() {
        return generateReservationRollback(faker, BigDecimal.valueOf(faker.number().numberBetween(1, 1_000_000)));
    }

    public static ReservationRollbackEntity generateReservationRollbackEntity() {
        return generateReservationRollbackEntity(faker, BigDecimal.valueOf(faker.number().numberBetween(1, 1_000_000)));
    }

    public static ReservationRollback generateReservationRollback(Faker faker, BigDecimal amount) {
        return new ReservationRollback(
            UUID.randomUUID(),
            UUID.randomUUID(),
            generatePan(faker),
            amount,
            generateRrn(faker),
            Instant.now()
        );
    }

    public static ReservationRollbackEntity generateReservationRollbackEntity(Faker faker, BigDecimal amount) {
        var reservationEntity = generateReservationEntity(faker, amount);
        return new ReservationRollbackEntity(
            UUID.randomUUID(),
            reservationEntity,
            generatePan(faker),
            amount,
            generateRrn(faker),
            Instant.now()
        );
    }

    public static String formatDateToStrExpiryDate(Instant instant) {
        return formatDateToStrExpiryDate(YearMonth.from(instant.atOffset(ZoneOffset.UTC)));
    }

    public static String formatDateToStrExpiryDate(YearMonth date) {
        return String.format("%02d%02d", date.getMonthValue(), date.getYear() % 100);
    }

    public static YearMonth strExpiryDateToYearMonth(String strExpiryDate) {
        if (strExpiryDate == null || strExpiryDate.length() != 4) {
            throw new IllegalArgumentException("Invalid expiry date format. Expected MMyy");
        }

        int month = Integer.parseInt(strExpiryDate.substring(0, 2));
        int shortYear = Integer.parseInt(strExpiryDate.substring(2, 4));
        int currentCentury = (LocalDate.now().getYear() / 100) * 100;
        int fullYear = currentCentury + shortYear;

        return YearMonth.of(fullYear, month);
    }

    public static String generatePan() {
        return generatePan(faker);
    }

    public static String generatePan(Faker faker) {
        return faker.number().digits(16);
    }

    public static String generatePan(Faker faker, String bin) {
        return bin + faker.number().digits(10);
    }

    public static String generateBin() {
        return generateBin(faker);
    }

    public static String generateBin(Faker faker) {
        return faker.number().digits(6);
    }

    public static String generateCardholderName() {
        return generateCardholderName(faker);
    }

    public static String generateCardholderName(Faker faker) {
        return faker.name().fullName().toUpperCase(Locale.ROOT);
    }

    public static String generateCurrencyCode() {
        return generateCurrencyCode(faker);
    }

    public static String generateCurrencyCode(Faker faker) {
        return faker.number().digits(3);
    }

    public static String generateIssuerId(Faker faker) {
        return faker.number().digits(3);
    }

    public static String generateRrn() {
        return generateRrn(faker);
    }

    public static String generateRrn(Faker faker) {
        return faker.number().digits(12);
    }
}
