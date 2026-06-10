package com.processing.terminalsimulator.service;

import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.AuthorizationResponse;
import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.dto.RunResponse;
import com.processing.terminalsimulator.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.ZonedDateTime;
import java.util.List;

import static com.processing.terminalsimulator.model.CardStatus.ACTIVE;
import static com.processing.terminalsimulator.model.CardStatus.BLOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalSimulatorServiceTest {

    @Mock
    private GatewayClient gatewayClient;

    @InjectMocks
    private TerminalSimulatorService service;

    private Card activeCard;
    private Card blockedCard;
    private List<Card> activeCards;
    private List<Card> blockedCards;

    @BeforeEach
    void setUp() {
        activeCard = new Card("id1", "4000001234560001", "400000",
                "IVAN IVANOV", "1228", ACTIVE, "643", 500_002,
                100_000, 20_000_000, "ISS001", "2026-06-01T10:00:00Z");
        blockedCard = new Card("id2", "4000001234560003", "400000",
                "PETR PETROV", "1228", BLOCKED, "643", 700_000,
                200_000, 40_000, "ISS001", "2026-06-01T10:00:00Z");

        activeCards = List.of(activeCard);
        blockedCards = List.of(blockedCard);

        when(gatewayClient.getCardsFromCardManager(ACTIVE, 70)).thenReturn(activeCards);
        when(gatewayClient.getCardsFromCardManager(BLOCKED, 30)).thenReturn(blockedCards);
        when(gatewayClient.sendToGateway(any(AuthorizationRequest.class)))
                .thenReturn(new AuthorizationResponse("", "", "", "",
                        "", "", "", 0));
    }

    private boolean isDayTime(AuthorizationRequest req) {
        int hour = ZonedDateTime.parse(req.transmissionDateTime()).getHour();
        return (hour >= 9 && hour < 22);
    }

    private boolean isNightTime(AuthorizationRequest req) {
        int hour = ZonedDateTime.parse(req.transmissionDateTime()).getHour();
        return (hour >= 1 && hour < 5);
    }

    private boolean isNormal(AuthorizationRequest req) {
        long amount = req.amount();
        if (amount < 10_000 || amount > 500_000) return false;
        if (!"5411".equals(req.mcc())) return false;
        if (!req.pan().equals(activeCard.pan())) return false;
        return isDayTime(req);
    }

    private boolean isNight(AuthorizationRequest req) {
        if (!req.pan().equals(activeCard.pan())) return false;
        return isNightTime(req);
    }

    private boolean isHighValue(AuthorizationRequest req) {
        long amount = req.amount();
        return amount >= 10_000_000 && amount <= 50_000_000 && req.pan().equals(activeCard.pan());
    }

    private boolean isDailyLimitExact(AuthorizationRequest req) {
        return req.pan().equals(activeCard.pan()) && req.amount() == activeCard.dailyLimit() - 1;
    }

    private boolean isBlocked(AuthorizationRequest req) {
        return req.pan().equals(blockedCard.pan());
    }

    private boolean isInvalidPan(AuthorizationRequest req, String expectedInvalidPan) {
        return req.pan().equals(expectedInvalidPan);
    }

    private boolean isNoMoney(AuthorizationRequest req) {
        if (!req.pan().equals(activeCard.pan())) return false;
        return req.amount() > activeCard.availableBalance();
    }

    private boolean isMoreThanDailyLimit(AuthorizationRequest req) {
        if (!req.pan().equals(activeCard.pan())) return false;
        long amount = req.amount();
        return amount > activeCard.dailyLimit()
                && amount <= activeCard.availableBalance(); // иначе это noMoney
    }

    @Test
    void run_normalScenario_TerminalSimulatorIsAlive() {
        RunResponse response = service.run(5, Scenario.normal);

        assertEquals(5, response.totalSubmitted());
        assertThat(response.approved()).isZero();
        assertThat(response.declined()).isZero();
        assertThat(response.transactions()).hasSize(5);
        verify(gatewayClient, times(5)).sendToGateway(any(AuthorizationRequest.class));
    }

    @Test
    void run_normalScenario_capturesAndClassifiesTransactions() {
        int totalCount = 10;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.normal);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        long normalCount = allRequests.stream().filter(this::isNormal).count();
        assertEquals(totalCount, normalCount);
    }

    @Test
    void run_nightTimeScenario_capturesAndClassifiesTransactions() {
        int totalCount = 10;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.night_time);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();
        long nightCount = allRequests.stream().filter(this::isNight).count();

        assertEquals(totalCount, nightCount);
    }

    @Test
    void run_highValueScenario_capturesAndClassifiesTransactions() {
        int totalCount = 10;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.high_value);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();
        long highValue = allRequests.stream().filter(this::isHighValue).count();

        assertEquals(totalCount, highValue);
    }

    @Test
    void run_mixedScenario_capturesAndClassifiesTransactions() {
        int totalCount = 19;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.mixed);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        long normalCount = allRequests.stream().filter(this::isNormal).count();
        long highValueCount = allRequests.stream().filter(this::isHighValue).count();
        long dailyLimitCount = allRequests.stream().filter(this::isDailyLimitExact).count();
        long blockedCount = allRequests.stream().filter(this::isBlocked).count();

        long expectedNormal = Math.round(totalCount * 0.7);
        long expectedHighValue = Math.round(totalCount * 0.15);
        long expectedDailyLimit = Math.round(totalCount * 0.10);
        long expectedBlocked = Math.round(totalCount * 0.05);
        int tolerance = 2;

        assertThat(normalCount).isBetween(expectedNormal - tolerance, expectedNormal + tolerance);
        assertThat(highValueCount).isBetween(expectedHighValue - tolerance, expectedHighValue + tolerance);
        assertThat(dailyLimitCount).isBetween(expectedDailyLimit - tolerance, expectedDailyLimit + tolerance);
        assertThat(blockedCount).isBetween(expectedBlocked - tolerance, expectedBlocked + tolerance);
        assertEquals(totalCount, normalCount + highValueCount + dailyLimitCount + blockedCount);
    }

    @Test
    void run_declinesScenario_capturesAndClassifiesTransactions() {
        int totalCount = 7;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.declines_test);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        String invalidPan = "4000001234560000";

        long invalidPanCount = allRequests.stream().filter(r -> isInvalidPan(r, invalidPan)).count();
        long blockedCount = allRequests.stream().filter(this::isBlocked).count();
        long noMoneyCount = allRequests.stream().filter(this::isNoMoney).count();
        long moreThanDailyCount = allRequests.stream().filter(this::isMoreThanDailyLimit).count();
        long normalCount = allRequests.stream().filter(this::isNormal).count();

        long expected = totalCount / 5;
        int tolerance = 2;

        assertThat(invalidPanCount).isBetween(expected - tolerance, expected + tolerance);
        assertThat(blockedCount).isBetween(expected - tolerance, expected + tolerance);
        assertThat(noMoneyCount).isBetween(expected - tolerance, expected + tolerance);
        assertThat(moreThanDailyCount).isBetween(expected - tolerance, expected + tolerance);
        assertThat(normalCount).isBetween(expected - tolerance, expected + tolerance);
        assertEquals(totalCount, invalidPanCount + blockedCount + noMoneyCount + moreThanDailyCount + normalCount);
    }

    @Test
    void run_whenNoActiveCards_throwsException() {
        when(gatewayClient.getCardsFromCardManager(ACTIVE, 70)).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, Scenario.normal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ACTIVE cards available");
    }

    @Test
    void run_whenNoBlockedCardsAndScenarioRequiresThem_throwsException() {
        when(gatewayClient.getCardsFromCardManager(BLOCKED, 30)).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, Scenario.mixed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No BLOCKED cards available");
    }

    @Test
    void stanCounter_shouldGenerateSequentialNumbers() {
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(1_000_000, Scenario.normal);

        verify(gatewayClient, times(1_000_000)).sendToGateway(captor.capture());
        List<AuthorizationRequest> requests = captor.getAllValues();

        String stan1 = requests.get(0).stan();
        String stan2 = requests.get(999_998).stan();
        String stan3 = requests.get(999_999).stan();

        assertEquals("000001", stan1);
        assertEquals("999999", stan2);
        assertEquals("000001", stan3);
    }

    @Test
    void normalScenario_shouldRespectAmountBoundaries() {
        int totalCount = 100;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.normal);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        for (AuthorizationRequest req : allRequests) {
            long amount = req.amount();
            assertThat(amount).isBetween(10_000L, 500_000L);
            assertThat(amount).isNotEqualTo(activeCard.dailyLimit() - 1);
        }
    }

    @Test
    void getInvalidPan_shouldFlipLastDigit() {
        int totalCount = 10;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.declines_test);

        verify(gatewayClient, atLeastOnce()).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        String validPan = activeCard.pan();
        String blockedPan = blockedCard.pan();
        AuthorizationRequest invalidRequest = allRequests.stream()
                .filter(req -> !req.pan().equals(validPan) && !req.pan().equals(blockedPan))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No invalid PAN transaction found"));
        String invalidPan = invalidRequest.pan();
        char lastValid = validPan.charAt(validPan.length() - 1);
        char expectedLast = (lastValid == '0') ? '1' : '0';
        char lastInvalid = invalidPan.charAt(invalidPan.length() - 1);

        assertEquals(validPan.length(), invalidPan.length());
        assertEquals(validPan.substring(0, validPan.length() - 1), invalidPan.substring(0, invalidPan.length() - 1));
        assertEquals(expectedLast, lastInvalid);
        assertEquals(validPan.substring(0, validPan.length() - 1),
                invalidPan.substring(0, invalidPan.length() - 1));
    }
}
