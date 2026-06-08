package com.processing.terminalsimulator.service;
import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.AuthorizationResponse;
import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.dto.RunResponse;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.processing.terminalsimulator.model.CardStatus.ACTIVE;
import static com.processing.terminalsimulator.model.CardStatus.BLOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class TerminalSimulatorServiceTest {
    @Mock
    private GatewayClient gatewayClient;
    @InjectMocks
    private TerminalSimulatorService service;
    private List<Card> cards;
    @BeforeEach
    void setUp() {
        var activeCard1 = new Card("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e", "4000001234560001", "400000",
                "IVAN IVANOV", "1228", ACTIVE, "643", 5000,
                100_000, 20_000, "ISS001", "2026-06-01T10:00:00Z");
        var activeCard2 = new Card("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d60", "4000001234560002", "400000",
                "IVAN IVANOV", "1228", ACTIVE, "643", 3000,
                100_000, 20_000, "ISS001", "2026-06-01T10:00:00Z");
        var blockedCard = new Card("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e", "4000001234560003", "400000",
                "PETR PETROV", "1228", BLOCKED, "643", 100_000,
                200_000, 20_000, "ISS001", "2026-06-01T10:00:00Z");
        cards = List.of(activeCard1, activeCard2, blockedCard);


        when(gatewayClient.getCardsFromCardManager(ACTIVE, 70)).thenReturn(List.of(activeCard1, activeCard2));
        when(gatewayClient.getCardsFromCardManager(BLOCKED, 30)).thenReturn(List.of(blockedCard));
        when(gatewayClient.sendToGateway(any(AuthorizationRequest.class)))
                .thenReturn(new AuthorizationResponse("", "", "", "",
                        "", "", "", 0));
    }

    public boolean isRequiredTransaction(AuthorizationRequest req, long start, long end, CardStatus cardStatus,
                                         String expectedPartOfDay, String invalidPan, String mcc, Boolean dailyLimit,
                                         Boolean noMoney, Boolean moreDaily) {
        long amount = req.amount();
        Card card = cards.stream().filter(c -> c.pan().equals(req.pan())).findFirst().orElse(null);
        if ((start != 0 && end != 0) && (amount < start || amount > end)) {
            return false;
        }
        if (mcc != null && !mcc.equals(req.mcc())) {
            return false;
        }
        boolean isRequiredStatus = (card != null) && card.status() == cardStatus;
        if (!isRequiredStatus) {
            return false;
        }
        if (expectedPartOfDay != null) {
            String tdt = req.transmissionDateTime();
            if (tdt == null) return false;
            ZonedDateTime dateTime = ZonedDateTime.parse(req.transmissionDateTime());
            int hour = dateTime.getHour();
            if ("day".equals(expectedPartOfDay)) {
                if (hour < 9 || hour >= 22) return false;
            } else if ("night".equals(expectedPartOfDay)) {
                if (hour < 1 || hour >= 5) return false;
            } else {
                return false;
            }
        }
        if (invalidPan != null) {
            return req.pan().equals(invalidPan);
        }
        if (noMoney) {
            return req.amount() > card.availableBalance();
        }
        if (dailyLimit) {  // лимит у тестовых карт меньше чем нижняя граница normal сценария
            return req.amount() == card.dailyLimit()-1;
        }
        if (moreDaily) {
            return req.amount() > card.dailyLimit();
        }
        return true;
    }

    @Test
    void run_normalScenario_TerminalSimulatorNotSetStatusTransactionByItself() {

        RunResponse response = service.run(5, Scenario.normal);

        assertThat(response.totalSubmitted()).isEqualTo(5);
        assertThat(response.approved()).isZero();
        assertThat(response.declined()).isZero();
        assertThat(response.transactions()).hasSize(5);
        verify(gatewayClient, times(5)).sendToGateway(any(AuthorizationRequest.class));
    }

    @Test
    void run_mixedScenario_capturesAndClassifiesTransactions() {
        int totalCount = 100;
        ArgumentCaptor<AuthorizationRequest> captor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        service.run(totalCount, Scenario.mixed);

        verify(gatewayClient, times(totalCount)).sendToGateway(captor.capture());
        List<AuthorizationRequest> allRequests = captor.getAllValues();

        long normalCount = allRequests.stream().filter(req -> isRequiredTransaction(req, 10_000,
                500_000, ACTIVE, "day",  null,"5411", false, false, false)).count();
        long highValueCount = allRequests.stream().filter(req -> isRequiredTransaction(req, 10_000_000,
                50_000_000, ACTIVE, null,  null,null, false, false, false)).count();
        long dailyLimitCount = allRequests.stream().filter(req -> isRequiredTransaction(req, 0,
                0, ACTIVE, null,  null,null, true, false, false)).count();
        long blockedCount = allRequests.stream().filter(req -> isRequiredTransaction(req, 0,
                0, BLOCKED, null,  null,null, false, false, false)).count();

        long expectedNormal = (long) (totalCount * 0.7);
        long expectedHighValue = (long) (totalCount * 0.15);
        long expectedDailyLimit = (long) (totalCount * 0.10);
        long expectedBlocked = (long) (totalCount * 0.05);

        assertThat(normalCount).isBetween(expectedNormal - 1, expectedNormal + 1);
        assertThat(highValueCount).isBetween(expectedHighValue - 1, expectedHighValue + 1);
        assertThat(dailyLimitCount).isBetween(expectedDailyLimit - 1, expectedDailyLimit + 1);
        assertThat(blockedCount).isBetween(expectedBlocked - 1, expectedBlocked + 1);

        assertThat(normalCount + highValueCount + dailyLimitCount + blockedCount)
                .isEqualTo(totalCount);
    }

}
