package com.processing.cardmanagement.components;

import com.processing.cardmanagement.events.*;
import com.processing.cardmanagement.models.*;
import com.processing.cardmanagement.options.*;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.ReservationRepository;
import com.processing.cardmanagement.repositories.ReservationRollbackRepository;
import com.processing.cardmanagement.services.*;
import com.processing.cardmanagement.services.retries.RetryServiceImpl;
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
        10000,
        3
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

    private final List<CardEventListener> listeners = Stream
        .generate(() -> Mockito.mock(CardEventListener.class))
        .limit(5)
        .toList();

    @Mock
    private OutboxEventProcessor outboxEventProcessor;

    @Mock
    private PanGenerator panGenerator;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationRollbackRepository reservationRollbackRepository;

    private final ArgumentCaptor<CardEvent> eventCaptor = ArgumentCaptor.forClass(CardEvent.class);
    private final ArgumentCaptor<CardOutboxEventData> outboxEventDataCaptor =
        ArgumentCaptor.forClass(CardOutboxEventData.class);

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
        CardEventNotifier eventNotifier = new CardEventNotifierImpl(
            outboxEventProcessor,
            List.of()
        );

        cardService = new CardServiceImpl(
            cardRepository,
            reservationRepository,
            reservationRollbackRepository,
            cardServiceSettings,
            cardServiceDefaults,
            panGenerator,
            eventNotifier,
            binIssuerService,
            new RetryServiceImpl(),
            new TransactionRunnerImpl()
        );

        cardGeneratorService = new CardGeneratorService(
            cardService,
            cardGeneratorOptions,
            eventNotifier
        );

        lenient().when(cardRepository.findByPan(anyString())).thenReturn(Optional.of(TEST_CARD));
        lenient().when(cardRepository.findByPanForUpdate(anyString())).thenReturn(Optional.of(TEST_CARD));
        lenient().when(cardRepository.create(any(Card.class))).thenReturn(TEST_CARD);
        lenient().when(cardRepository.update(any(Card.class))).thenReturn(TEST_CARD);
        lenient().when(cardRepository.createAll(any())).thenReturn(List.of(TEST_CARD));
        lenient().when(reservationRepository.save(any(Reservation.class))).thenReturn(TEST_RESERVATION);
        lenient().when(reservationRepository.findByRrnAndPanForUpdate(anyString(), anyString())).thenReturn(Optional.of(TEST_RESERVATION));
        lenient().when(reservationRepository.isUnique(anyString(), anyString())).thenReturn(true);
        lenient().when(reservationRollbackRepository.save(any(ReservationRollback.class))).thenReturn(TEST_ROLLBACK);
        lenient().when(outboxEventProcessor.save(any(CardOutboxEventData.class))).thenReturn(null);
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
        when(reservationRepository.findByRrnAndPanForUpdate(anyString(), anyString()))
            .thenReturn(Optional.of(TEST_RESERVATION));
        cardService.rollback("1234123412341234", BigDecimal.ONE, "123412341234");
        testAllListenersReceivedData(CardServiceRollbackEvent.class);
    }

    @Test
    void cardsBatchGeneratedEventTest() {
        cardGeneratorService.generate(1, List.of("123456"));
        verify(outboxEventProcessor, times(2)).save(outboxEventDataCaptor.capture());
        var captures = outboxEventDataCaptor.getAllValues();
        assertInstanceOf(CardServiceCreationEvent.class, captures.getFirst().event());
        assertInstanceOf(CardsBatchGeneratedEvent.class, captures.getLast().event());
    }

    private <T> void testAllListenersReceivedData(Class<T> expectedType) {
        verify(outboxEventProcessor).save(outboxEventDataCaptor.capture());
        assertInstanceOf(expectedType, outboxEventDataCaptor.getValue().event());
    }
}
