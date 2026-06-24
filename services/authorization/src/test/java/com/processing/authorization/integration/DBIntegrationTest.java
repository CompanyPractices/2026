package com.processing.authorization.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.processing.authorization.client.CardManagementClient;
import com.processing.authorization.entities.LimitUsage;
import com.processing.authorization.exceptions.ReserveException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.services.AuthServiceImpl;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class DBIntegrationTest {
    @Autowired
    private AuthServiceImpl authService;
    @Autowired
    private LimitUsageRepository limitUsageRepository;

    @MockitoBean
    private CardManagementClient cardManagementClient;

    private Instant now;
    private AuthorizationRequest correctRequest;

    private static final String TEST_PAN = "1234567890123456";

    @BeforeEach
    void setUp() {
        limitUsageRepository.deleteAll();
        now = Instant.now();
        correctRequest = new AuthorizationRequest(
                "0100",
                "123456",
                TEST_PAN,
                "000000",
                BigDecimal.valueOf(5000),
                "810",
                Instant.parse("2026-06-05T18:12:49.070Z"),
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");
        reset(cardManagementClient);
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsBlocked() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createBlockedCard());

        AuthorizationResponse response = authService.authorize(correctRequest, now);

        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertThat(response.authCode()).isNull();
        assertThat(response.rrn()).isNull();
        assertEquals("CARD_BLOCKED", response.declineReason());
        verify(cardManagementClient, never()).reserve(any(), any(), any());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsExpired() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createExpiredCardModel());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardIsInactive() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createInactiveCardModel());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_INACTIVE", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenInsufficientFunds() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createCardWithLowBalance());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenExceededMonthlyLimit() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createActiveCardModelWithLowMonthlyLimit());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("EXCEEDS_AMOUNT_LIMIT", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenReservationFails() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createActiveCard());
        doThrow(new ReserveException("Reserve failed")).when(cardManagementClient)
                .reserve(any(BigDecimal.class), anyString(), anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("RESERVATION_FAILED", response.declineReason());
    }

    @Test
    void authorizeShouldReturnDeclinedWhenCardExpiredByDate() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createCardExpiredByDate());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_DECLINED, response.status());
        assertEquals("CARD_EXPIRED", response.declineReason());
    }

    @Test
    void authorizeShouldBeApproved() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createActiveCard());
        doNothing().when(cardManagementClient).reserve(any(BigDecimal.class), anyString(), anyString());

        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(response.authCode()).isNotNull();
        assertThat(response.rrn()).isNotNull();

        verify(cardManagementClient).getCard(TEST_PAN);
        verify(cardManagementClient).reserve(eq(BigDecimal.valueOf(5000)), anyString(), eq(TEST_PAN));
    }

    @Test
    void authorizeShouldBeApprovedAndPersistToDatabase() {
        when(cardManagementClient.getCard(anyString())).thenReturn(createActiveCard());
        doNothing().when(cardManagementClient).reserve(any(BigDecimal.class), anyString(), anyString());
        AuthorizationResponse response = authService.authorize(correctRequest, now);
        assertEquals(AuthorizationResponse.STATUS_APPROVED, response.status());
        assertThat(limitUsageRepository.findAll()).hasSize(1);
    }

    @Test
    @Transactional
    void deleteByUsageDateBetweenShouldDeleteOnlyPreviousMonthRecords() {
        String pan = "4000001234567890";
        BigDecimal currLimit = BigDecimal.valueOf(1000);

        LocalDate now = LocalDate.now();
        LocalDate firstDayPreviousMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayPreviousMonth = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());
        LocalDate firstDayCurrentMonth = now.withDayOfMonth(1);
        LocalDate secondDayCurrentMonth = now.withDayOfMonth(2);

        LimitUsage record1 = createLimitUsage(pan, firstDayPreviousMonth, currLimit);
        LimitUsage record2 = createLimitUsage(pan, lastDayPreviousMonth.minusDays(1), currLimit);
        LimitUsage record3 = createLimitUsage(pan, lastDayPreviousMonth, currLimit);
        LimitUsage record4 = createLimitUsage(pan, firstDayCurrentMonth, currLimit);
        LimitUsage record5 = createLimitUsage(pan, secondDayCurrentMonth, currLimit);
        limitUsageRepository.saveAll(List.of(record1, record2, record3, record4, record5));

        List<LimitUsage> allRecords = limitUsageRepository.findAll();
        assertThat(allRecords).hasSize(5);

        int deletedCount = limitUsageRepository.deleteByUsageDateBetween(
                firstDayPreviousMonth,
                lastDayPreviousMonth
        );

        assertThat(deletedCount).isEqualTo(3);

        List<LimitUsage> remainingRecords = limitUsageRepository.findAll();
        assertThat(remainingRecords).hasSize(2);
        assertThat(remainingRecords)
                .extracting("usageDate")
                .containsExactlyInAnyOrder(firstDayCurrentMonth, secondDayCurrentMonth);
    }

    private CardModel createCard(CardModelStatus status, YearMonth expiryDate,
                                 BigDecimal availableBalance, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
        return new CardModel(
                UUID.randomUUID(),
                TEST_PAN,
                "123456",
                "John Golt",
                expiryDate,
                status,
                "810",
                dailyLimit,
                monthlyLimit,
                availableBalance,
                "I001",
                Instant.now()
        );
    }

    private CardModel createActiveCard() {
        return createCard(
                CardModelStatus.ACTIVE,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createBlockedCard() {
        return createCard(
                CardModelStatus.BLOCKED,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createExpiredCardModel() {
        return createCard(
                CardModelStatus.EXPIRED,
                YearMonth.of(2026, 1),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createInactiveCardModel() {
        return createCard(
                CardModelStatus.INACTIVE,
                YearMonth.of(2029, 1),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createCardWithLowBalance() {
        return createCard(
                CardModelStatus.ACTIVE,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createActiveCardModelWithLowMonthlyLimit() {
        return createCard(
                CardModelStatus.ACTIVE,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(10000)
        );
    }

    private CardModel createCardExpiredByDate() {
        return createCard(
                CardModelStatus.ACTIVE,
                YearMonth.of(2006, 12),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000)
        );
    }

    private LimitUsage createLimitUsage(String pan, LocalDate usageDate, BigDecimal amount) {
        LimitUsage usage = new LimitUsage();
        usage.setPan(pan);
        usage.setUsageDate(usageDate);
        usage.setDailyAmount(amount);
        usage.setMonthlyAmount(amount);
        usage.setUpdatedAt(Instant.now());
        return usage;
    }

    @Test
    void generateRRNReturnUniqueValues() {
        AuthorizationRequest duplicateRequest = new AuthorizationRequest(
                correctRequest.mti(),
                "123457",
                correctRequest.pan(),
                correctRequest.processingCode(),
                correctRequest.amount(),
                correctRequest.currencyCode(),
                correctRequest.transmissionDateTime(),
                correctRequest.terminalId(),
                correctRequest.terminalType(),
                correctRequest.merchantId(),
                correctRequest.mcc(),
                correctRequest.acquirerId(),
                correctRequest.issuerId());
        String rrn1 = authService.generateRRN();
        String rrn2 = authService.generateRRN();

        assertThat(rrn1).isNotBlank();
        assertThat(rrn2).isNotBlank();
        assertThat(rrn1).isNotEqualTo(rrn2);
        assertThat(rrn1).hasSize(12);
        assertThat(rrn2).hasSize(12);
    }

    @Test
    void generateRRNShouldGenerate1001UniqueValues() {
        AuthorizationRequest request = new AuthorizationRequest(
                correctRequest.mti(),
                "123457",
                correctRequest.pan(),
                correctRequest.processingCode(),
                correctRequest.amount(),
                correctRequest.currencyCode(),
                correctRequest.transmissionDateTime(),
                correctRequest.terminalId(),
                correctRequest.terminalType(),
                correctRequest.merchantId(),
                correctRequest.mcc(),
                correctRequest.acquirerId(),
                correctRequest.issuerId());
        int count = 1001;
        Set<String> rrns = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String rrn = authService.generateRRN();
            assertThat(rrn).isNotBlank();
            assertThat(rrn).hasSize(12);
            assertThat(rrns).doesNotContain(rrn);
            rrns.add(rrn);
        }

        assertThat(rrns).hasSize(count);
    }

    @Test
    void generateRRNShouldUseNextBlockAfter1000Values() {
        int count = 1001;
        List<String> rrns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rrns.add(authService.generateRRN());
        }

        assertThat(new HashSet<>(rrns)).hasSize(count);
        String firstOfFirstBlock = rrns.get(0);
        String firstOfSecondBlock = rrns.get(1000);
        long firstNum = Long.parseLong(firstOfFirstBlock.substring(4));
        long secondBlockNum = Long.parseLong(firstOfSecondBlock.substring(4));
        assertThat(secondBlockNum / 1000).isEqualTo(firstNum / 1000 + 1);
    }

}
