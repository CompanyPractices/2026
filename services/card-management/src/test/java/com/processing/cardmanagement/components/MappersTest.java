package com.processing.cardmanagement.components;

import com.processing.cardmanagement.mappers.*;
import com.processing.cardmanagement.models.*;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;

import static com.processing.cardmanagement.utils.CardManagementTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappersTest {

    private final Faker faker = new Faker();
    private final CardExpiryDateMapper expiryDateMapper =
        new CardExpiryDateMapper();
    private final CardPersistenceMapper cardPersistenceMapper =
        Mappers.getMapper(CardPersistenceMapper.class);

    // Don't want to use ReflectionAPI in @BeforeAll :(
    {
        try {
            var field = cardPersistenceMapper.getClass().getDeclaredField("cardExpiryDateMapper");
            field.setAccessible(true);
            field.set(cardPersistenceMapper, expiryDateMapper);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private final CardStatusMapper cardStatusMapper = Mappers.getMapper(CardStatusMapper.class);
    private final CardRestMapper cardRestMapper = Mappers.getMapper(CardRestMapper.class);

    {
        try {
            var field = cardRestMapper.getClass().getDeclaredField("cardStatusMapper");
            field.setAccessible(true);
            field.set(cardRestMapper, cardStatusMapper);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private final ReservationPersistenceMapper reservationPersistenceMapper =
        Mappers.getMapper(ReservationPersistenceMapper.class);
    private final ReservationRollbackPersistenceMapper reservationRollbackPersistenceMapper =
        Mappers.getMapper(ReservationRollbackPersistenceMapper.class);

    @Test
    void expiryDateMapperToStringTest() {
        var date = createRandomYearMonthDate();
        assertEquals(
            formatDateToStrExpiryDate(date),
            expiryDateMapper.asString(date)
        );
    }

    @Test
    void expiryDateMapperToDateTest() {
        var date = createRandomStrDate();
        assertEquals(
            strExpiryDateToYearMonth(date),
            expiryDateMapper.asYearMonth(date)
        );
    }

    @Test
    void expiryDateMapperYearMonthSelfTest() {
        var date = createRandomYearMonthDate();
        var strDate = expiryDateMapper.asString(date);
        assertEquals(
            date,
            expiryDateMapper.asYearMonth(strDate)
        );
    }

    @Test
    void expiryDateMapperStrSelfTest() {
        var strDate = createRandomStrDate();
        var date = expiryDateMapper.asYearMonth(strDate);
        assertEquals(
            strDate,
            expiryDateMapper.asString(date)
        );
    }

    @Test
    void cardPersistenceMapperToEntityTest() {
        var card = generateActiveCard();
        var cardEntity = cardPersistenceMapper.toEntity(card);
        var expectedEntity = new CardEntity(
            card.id(),
            card.pan(),
            card.bin(),
            card.cardholderName(),
            formatDateToStrExpiryDate(card.expiryDate()),
            card.status().name(),
            card.currencyCode(),
            card.dailyLimit(),
            card.monthlyLimit(),
            card.availableBalance(),
            card.issuerId(),
            card.createdAt()
        );
        assertEquals(expectedEntity, cardEntity);
    }

    @Test
    void cardPersistenceMapperToDomainTest() {
        var cardEntity = generateActiveCardEntity();
        var card = cardPersistenceMapper.toDomain(cardEntity);
        var expectedCard = new Card(
            cardEntity.getId(),
            cardEntity.getPan(),
            cardEntity.getBin(),
            cardEntity.getCardholderName(),
            strExpiryDateToYearMonth(cardEntity.getExpiryDate()),
            CardStatus.valueOf(cardEntity.getStatus()),
            cardEntity.getCurrencyCode(),
            cardEntity.getDailyLimit(),
            cardEntity.getMonthlyLimit(),
            cardEntity.getAvailableBalance(),
            cardEntity.getIssuerId(),
            cardEntity.getCreatedAt()
        );
        assertEquals(expectedCard, card);
    }

    @Test
    void cardPersistenceMapperDomainSelfTest() {
        var card = generateActiveCard();
        var entity = cardPersistenceMapper.toEntity(card);
        assertEquals(card, cardPersistenceMapper.toDomain(entity));
    }

    @Test
    void cardPersistenceMapperEntitySelfTest() {
        var entity = generateActiveCardEntity();
        var card = cardPersistenceMapper.toDomain(entity);
        assertEquals(entity, cardPersistenceMapper.toEntity(card));
    }

    @Test
    void cardStatusMapperToModelTest() {
        var statuses = Arrays.stream(CardStatus.values())
            .filter(status -> status != CardStatus.DELETED)
            .toArray(CardStatus[]::new);
        var status = faker.options().option(statuses);
        var modelStatus = cardStatusMapper.toCardModelStatus(status);

        assertEquals(status, CardStatus.valueOf(modelStatus.name()));
    }

    @Test
    void cardStatusMapperToDomainTest() {
        var modelStatus = faker.options().option(CardModelStatus.class);
        var status = cardStatusMapper.toCardStatus(modelStatus);

        assertEquals(modelStatus, CardModelStatus.valueOf(status.name()));
    }

    @Test
    void cardStatusMapperDomainSelfTest() {
        var status = CardStatus.ACTIVE;
        var model = cardStatusMapper.toCardModelStatus(status);
        assertEquals(status, cardStatusMapper.toCardStatus(model));
    }

    @Test
    void cardStatusMapperModelSelfTest() {
        var model = CardModelStatus.ACTIVE;
        var status = cardStatusMapper.toCardStatus(model);
        assertEquals(model, cardStatusMapper.toCardModelStatus(status));
    }

    @Test
    void cardRestMapperToModelTest() {
        var card = generateActiveCard();
        var model = cardRestMapper.toDto(card);

        var expectedModel = new CardModel(
            card.id(),
            card.pan(),
            card.bin(),
            card.cardholderName(),
            card.expiryDate(),
            CardModelStatus.valueOf(card.status().name()),
            card.currencyCode(),
            card.dailyLimit(),
            card.monthlyLimit(),
            card.availableBalance(),
            card.issuerId(),
            card.createdAt()
        );
        assertEquals(expectedModel, model);
    }

    @Test
    void cardRestMapperToDomainTest() {
        var model = generateActiveCardModel();
        var card = cardRestMapper.toDomain(model);

        var expectedCard = new Card(
            model.id(),
            model.pan(),
            model.bin(),
            model.cardholderName(),
            model.expiryDate(),
            CardStatus.valueOf(model.status().name()),
            model.currencyCode(),
            model.dailyLimit(),
            model.monthlyLimit(),
            model.availableBalance(),
            model.issuerId(),
            model.createdAt()
        );
        assertEquals(expectedCard, card);
    }

    @Test
    void cardRestMapperDomainSelfTest() {
        var card = generateActiveCard();
        var model = cardRestMapper.toDto(card);
        assertEquals(card, cardRestMapper.toDomain(model));
    }

    @Test
    void cardRestMapperModelSelfTest() {
        var model = generateActiveCardModel();
        var card = cardRestMapper.toDomain(model);
        assertEquals(model, cardRestMapper.toDto(card));
    }

    @Test
    void reservationPersistenceMapperToEntityTest() {
        var reservation = generateReservation();
        var entity = reservationPersistenceMapper.toEntity(reservation);

        var expectedEntity = new ReservationEntity(
            reservation.id(),
            reservation.pan(),
            reservation.reservationAmount(),
            reservation.rrn(),
            reservation.status().name(),
            reservation.updatedAt(),
            reservation.createdAt()
        );
        assertEquals(expectedEntity, entity);
    }

    @Test
    void reservationPersistenceMapperToDomainTest() {
        var entity = generateReservationEntity();
        var reservation = reservationPersistenceMapper.toDomain(entity);

        var expectedReservation = new Reservation(
            entity.getId(),
            entity.getPan(),
            entity.getReservationAmount(),
            entity.getRrn(),
            ReservationStatus.valueOf(entity.getStatus()),
            reservation.updatedAt(),
            entity.getCreatedAt()
        );
        assertEquals(expectedReservation, reservation);
    }

    @Test
    void reservationPersistenceMapperDomainSelfTest() {
        var reservation = generateReservation();
        var entity = reservationPersistenceMapper.toEntity(reservation);
        assertEquals(reservation, reservationPersistenceMapper.toDomain(entity));
    }

    @Test
    void reservationPersistenceMapperEntitySelfTest() {
        var entity = generateReservationEntity();
        var reservation = reservationPersistenceMapper.toDomain(entity);
        assertEquals(entity, reservationPersistenceMapper.toEntity(reservation));
    }

    @Test
    void reservationRollbackPersistenceMapperToEntityTest() {
        var rollback = generateReservationRollback();
        var entity = reservationRollbackPersistenceMapper.toEntity(rollback);
        var expectedReservationEntity = new ReservationEntity();
        expectedReservationEntity.setId(rollback.reservationId());

        var expectedEntity = new ReservationRollbackEntity(
            rollback.id(),
            expectedReservationEntity,
            rollback.pan(),
            rollback.rollbackAmount(),
            rollback.rrn(),
            rollback.createdAt()
        );
        assertEquals(expectedEntity, entity);
    }

    @Test
    void reservationRollbackPersistenceMapperToDomainTest() {
        var entity = generateReservationRollbackEntity();
        var rollback = reservationRollbackPersistenceMapper.toDomain(entity);

        var expectedRollback = new ReservationRollback(
            rollback.id(),
            entity.getReservation().getId(),
            rollback.pan(),
            rollback.rollbackAmount(),
            rollback.rrn(),
            rollback.createdAt()
        );
        assertEquals(expectedRollback, rollback);
    }

    @Test
    void reservationRollbackPersistenceMapperDomainSelfTest() {
        var rollback = generateReservationRollback();
        var entity = reservationRollbackPersistenceMapper.toEntity(rollback);
        assertEquals(rollback, reservationRollbackPersistenceMapper.toDomain(entity));
    }

    @Test
    void reservationRollbackPersistenceMapperEntitySelfTest() {
        var entity = generateReservationRollbackEntity();
        var rollback = reservationRollbackPersistenceMapper.toDomain(entity);

        assertEquals(entity, reservationRollbackPersistenceMapper.toEntity(rollback));
    }

    private YearMonth createRandomYearMonthDate() {
        return YearMonth.from(faker.timeAndDate().future().atOffset(ZoneOffset.UTC));
    }

    private String createRandomStrDate() {
        return String.format(
            "%02d%02d",
            faker.number().numberBetween(1, 12),
            faker.number().numberBetween(0, 99)
        );
    }
}
