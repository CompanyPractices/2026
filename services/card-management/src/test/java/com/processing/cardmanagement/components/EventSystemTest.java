package com.processing.cardmanagement.components;

import com.processing.cardmanagement.events.*;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.models.Reservation;
import com.processing.cardmanagement.models.ReservationRollback;
import com.processing.cardmanagement.options.*;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import com.processing.cardmanagement.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventSystemTest {

    private final CardServiceSettings cardServiceSettings = new CardServiceSettingsConfigurationProperties(
        3,
        10000
    );

    private final CardServiceDefaults cardServiceDefaults = new CardServiceDefaultsConfigurationProperties(
        1,
        50,
        "643",
        BigDecimal.valueOf(15000000),
        BigDecimal.valueOf(300000000),
        BigDecimal.valueOf(300000000)
    );

    private final CardGeneratorOptions cardGeneratorOptions = new CardGeneratorOptions(
        BigDecimal.ZERO,
        BigDecimal.valueOf(1_000_000),
        BigDecimal.ZERO,
        BigDecimal.valueOf(1_000_000),
        "643",
        10000
    );

    @Mock
    private PanGenerator panGenerator;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationRollbackRepository reservationRollbackRepository;

    List<CardEventListener> listeners = Stream
        .generate(() -> Mockito.mock(CardEventListener.class))
        .limit(5)
        .toList();

    private final ArgumentCaptor<CardEvent> eventCaptor = ArgumentCaptor.forClass(CardEvent.class);

    private final CardEventNotifier eventNotifier = new CardEventNotifier(listeners);

    @Mock
    private BinIssuerService binIssuerService;

    private CardService cardService;

    private CardGeneratorService cardGeneratorService;

    private final static Card TEST_CARD =
        new Card(
            UUID.randomUUID(),
            "1234123412341234",
            "123456",
            "ANY_CN",
            YearMonth.now().plusYears(3),
            CardStatus.ACTIVE,
            "643",
            BigDecimal.ONE,
            BigDecimal.TWO,
            BigDecimal.TEN,
            "ANYISSUER"
        );

    private static final Reservation TEST_RESERVATION =
        TEST_CARD.startReservation(BigDecimal.ONE, "123412341234");

    private static final ReservationRollback TEST_ROLLBACK =
        TEST_RESERVATION.startRollback(BigDecimal.ONE);

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
            cardRepository,
            reservationRepository,
            reservationRollbackRepository,
            cardServiceSettings,
            cardServiceDefaults,
            panGenerator,
            eventNotifier,
            binIssuerService
        );

        cardGeneratorService = new CardGeneratorService(
            cardService,
            cardGeneratorOptions,
            eventNotifier
        );


        lenient().when(cardRepository.findByPan(any(String.class))).thenReturn(Optional.of(TEST_CARD));
        lenient().when(cardRepository.findByPanForUpdate(any(String.class))).thenReturn(Optional.of(TEST_CARD));
        lenient().when(cardRepository.save(any(Card.class))).thenReturn(TEST_CARD);
        lenient().when(cardRepository.saveAll(any())).thenReturn(List.of(TEST_CARD));
        lenient().when(reservationRepository.save(any(Reservation.class))).thenReturn(TEST_RESERVATION);
        lenient().when(reservationRollbackRepository.save(any(ReservationRollback.class))).thenReturn(TEST_ROLLBACK);
    }

    @Test
    void cardServiceCreationEventTest() {
        cardService.createCard(
            "ANY_BIN",
            "ANY_CN",
            "123",
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
        );
        testAllListenersReceivedData(CardServiceCreationEvent.class);
    }

    @Test
    void cardServicePatchEventTest() {
        cardService.patchCard(
            "1234123412341234",
            CardStatus.ACTIVE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
        );
        testAllListenersReceivedData(CardServicePatchEvent.class);
    }

    @Test
    void cardServiceDeleteEventTest() {
        cardService.deleteCard("1234123412341234");
        testAllListenersReceivedData(CardServiceDeletionEvent.class);
    }

    @Test
    void cardServiceReserveEventTest() {
        cardService.reserve(
            "1234123412341234",
            BigDecimal.ONE,
            "123412341234"
        );
        testAllListenersReceivedData(CardServiceReserveEvent.class);
    }

    @Test
    void cardServiceRollbackEventTest() {
        when(reservationRepository.findByRrn(any(String.class))).thenReturn(Optional.of(TEST_RESERVATION));
        cardService.rollback("1234123412341234", BigDecimal.ONE, "123412341234");
        testAllListenersReceivedData(CardServiceRollbackEvent.class);
    }

    @Test
    void cardsBatchGeneratedEventTest() {
        cardGeneratorService.generate(1, List.of("123456"));
        for (var l : listeners) {
            verify(l, times(2)).onEvent(eventCaptor.capture());
            var captures = eventCaptor.getAllValues();
            assertInstanceOf(CardServiceCreationEvent.class, captures.getFirst());
            assertInstanceOf(CardsBatchGeneratedEvent.class, captures.getLast());
        }
    }

    private <T> void testAllListenersReceivedData(Class<T> expectedType) {
        for (var l : listeners) {
            verify(l).onEvent(eventCaptor.capture());
            assertInstanceOf(expectedType, eventCaptor.getValue());
        }
    }

}
