package com.processing.authorization.service;

import com.processing.authorization.exceptions.*;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.entities.LimitUsage;
import com.processing.authorization.repositories.LimitUsageRepository;
import com.processing.authorization.services.AuthService;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.utils.MaskPan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import static com.processing.authorization.constants.DeclineOutcome.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    private AuthorizationRequest correctRequest;

    private CardModel activeCardResponse;

    private RollbackRequest rollbackRequest;

    @Mock
    private LimitUsageRepository limitUsageRepository;

    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.create();
        authService = new AuthService(restClient, limitUsageRepository);

        correctRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                BigDecimal.valueOf(5000),
                "810",
                "2026-06-05T18:12:49.070",
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001");

        activeCardResponse = new CardModel(
                UUID.randomUUID(),
                "1234567890123456",
                "123456",
                "John Golt",
                YearMonth.of(2026, 12),
                CardModelStatus.ACTIVE,
                "810",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000L),
                BigDecimal.valueOf(10000),
                "I001",
                LocalDateTime.now());

        rollbackRequest = new RollbackRequest(
                "123456789012",
                "1234567890123456",
                BigDecimal.valueOf(5000)
        );
    }

    @Test
    void authorizeApprovedWhenAllChecksPassed() {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doNothing().when(spyService).reserve(any(BigDecimal.class), anyString(), anyString());
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(limitUsageRepository.findTopByPanAndUsageDateBetweenOrderByUsageDateDesc(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();

        verify(spyService, times(1)).getCard(correctRequest.pan());
        verify(spyService, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
    }

    @Test
    void authorizeReturnServiceUnavailableWhenGetCardThrowsException() {
        AuthService spyService = spy(authService);
        doThrow(new ServiceUnavailableException("Card Management service unavailable"))
                .when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(SERVICE_UNAVAILABLE.code());
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardExpired() {
        CardModel expiredCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
            CardModelStatus.EXPIRED,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(expiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenCardBlocked() {
        CardModel blockedCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.BLOCKED,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(blockedCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_BLOCKED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_BLOCKED.reason());
    }

    @Test
    void authorizeDeclineWhenCardInactive() {
        CardModel inactiveCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.INACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(inactiveCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_INACTIVE.code());
        assertThat(response.declineReason()).isEqualTo(CARD_INACTIVE.reason());
    }

    @Test
    void authorizeDeclineWhenCardStatusUnknown() {
        CardModel unknownStatusCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                null,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(unknownStatusCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(UNKNOWN_REASON.code());
        assertThat(response.declineReason()).isEqualTo(UNKNOWN_REASON.reason());
    }

    @Test
    void authorize_shouldDecline_whenExpiryDateInPastEvenIfCardActive() {
        YearMonth expiredDate = YearMonth.of(2023, 12);

        CardModel activeButExpiredCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                expiredDate,
                CardModelStatus.ACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                activeCardResponse.availableBalance(),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(activeButExpiredCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(CARD_EXPIRED.code());
        assertThat(response.declineReason()).isEqualTo(CARD_EXPIRED.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenInsufficientFunds() {
        CardModel lowBalanceCard = new CardModel(
                activeCardResponse.id(),
                activeCardResponse.pan(),
                activeCardResponse.bin(),
                activeCardResponse.cardholderName(),
                activeCardResponse.expiryDate(),
                CardModelStatus.ACTIVE,
                activeCardResponse.currencyCode(),
                activeCardResponse.dailyLimit(),
                activeCardResponse.monthlyLimit(),
                BigDecimal.valueOf(1000),
                activeCardResponse.issuerId(),
                activeCardResponse.createdAt());
        AuthService spyService = spy(authService);
        doReturn(lowBalanceCard).when(spyService).getCard(anyString());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(INSUFFICIENT_FUNDS.code());
        assertThat(response.declineReason()).isEqualTo(INSUFFICIENT_FUNDS.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenReserveThrowsException() {
        AuthService spyService = spy(authService);
        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doThrow(new ReserveException("Reserve failed")).when(spyService).reserve(any(BigDecimal.class), anyString(), anyString());
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(limitUsageRepository.findTopByPanAndUsageDateBetweenOrderByUsageDateDesc(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.responseCode()).isEqualTo(RESERVATION_FAILED.code());
        assertThat(response.declineReason()).isEqualTo(RESERVATION_FAILED.reason());
        verify(spyService, times(1)).reserve(eq(correctRequest.amount()), anyString(), eq(correctRequest.pan()));
    }

    @Test
    void generateRRNReturnUniqueValues() {
        String rrn1 = authService.generateRRN();
        String rrn2 = authService.generateRRN();

        assertThat(rrn1).isNotBlank();
        assertThat(rrn2).isNotBlank();
        assertThat(rrn1).isNotEqualTo(rrn2);
        assertThat(rrn1).hasSize(12);
        assertThat(rrn2).hasSize(12);
    }

    @Test
    void generateAuthCode_shouldReturnSixDigitAlphanumeric() {
        String code = authService.generateAuthCode();

        assertThat(code).hasSize(6);
        assertThat(code).matches("[0-9A-Z]{6}");
    }

    @Test
    void authorizeApprovedWhenDailyLimitNotReached() {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        doNothing().when(spyService).reserve(any(BigDecimal.class), anyString(), anyString());

        LimitUsage usage = new LimitUsage();
        usage.setDailyAmount(BigDecimal.valueOf(50000));
        usage.setMonthlyAmount(BigDecimal.valueOf(200000));
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));


        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isNotNull();
        assertThat(response.authCode()).isNotNull();
        assertThat(response.declineReason()).isNull();
    }

    @Test
    void authorizeDeclineWhenDailyLimitReached() {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());
        LimitUsage usage = new LimitUsage();
        usage.setDailyAmount(BigDecimal.valueOf(96000));
        usage.setMonthlyAmount(BigDecimal.valueOf(200000));
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void authorizeDeclineWhenMonthlyLimitReached() {
        AuthService spyService = spy(authService);

        doReturn(activeCardResponse).when(spyService).getCard(anyString());

        LimitUsage usage = new LimitUsage();
        usage.setDailyAmount(BigDecimal.valueOf(50000));
        usage.setMonthlyAmount(BigDecimal.valueOf(498000));
        when(limitUsageRepository.findByPanAndUsageDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));

        AuthorizationResponse response = spyService.authorize(correctRequest, LocalDateTime.now());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.code());
        assertThat(response.declineReason()).isEqualTo(EXCEEDS_AMOUNT_LIMIT.reason());
        verify(spyService, never()).reserve(any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void maskPAN_ShouldMaskMiddleEightCharacters() {
        String input = "4000001234560001";
        String result = MaskPan.maskPan(input);

        assertEquals("4000********0001", result);
        assertEquals(16, result.length());
    }

    @Test
    void maskPAN_ShouldReturnInvalidPanOnShortString() {
        String input = "4000";
        String result = MaskPan.maskPan(input);

        assertEquals("****", result);
    }

    @Test
    void maskPAN_ShouldReturnEmptyStringOnNull() {
        String input = null;
        String result = MaskPan.maskPan(input);

        assertEquals("", result);
    }

    @Test
    void maskPAN_ShouldPreserveOriginalLength() {
        String input1 = "4000001234560001";
        String result1 = MaskPan.maskPan(input1);

        String input2 = "400001";
        String result2 = MaskPan.maskPan(input2);

        assertEquals(input1.length(), result1.length());
        assertEquals(input2.length(), result2.length());
    }

    @Test
    void testAccumulatedRoundingError_withBigDecimalShouldBeExact() {
        BigDecimal limit = new BigDecimal("1000.00");
        BigDecimal amount = new BigDecimal("333.34");

        BigDecimal firstReserve = amount;
        BigDecimal secondReserve = amount;
        BigDecimal thirdReserve = amount;
        BigDecimal totalUsed = firstReserve.add(secondReserve).add(thirdReserve);

        boolean isExceeded = totalUsed.compareTo(limit) > 0;
        assertTrue(isExceeded, "Превышение лимита с типом long не было бы обнаружено");
    }

    @Test
    void rollbackReturnApprovedWhenCmsRollbackSucceeds() {
        AuthService spyService = spy(authService);

        doNothing().when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_APPROVED);
        assertThat(response.rrn()).isEqualTo("123456789012");
        assertThat(response.declineReason()).isNull();
        verify(spyService, times(1)).rollbackCard(rollbackRequest);
    }

    @Test
    void rollbackShouldReturnDeclinedWhenCmsReturnsInternalError() {
        AuthService spyService = spy(authService);

        doThrow(new InternalCardManagerException("Internal card management error"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenCardNotFound() {
        AuthService spyService = spy(authService);

        doThrow(new CardNotFoundException("Card not found"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(TRANSACTION_NOT_FOUND.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenAlreadyRolledBack() {
        AuthService spyService = spy(authService);

        doThrow(new RollbackConflictException("Rollback conflict"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(ALREADY_ROLLED_BACK.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenServiceUnavailable() {
        AuthService spyService = spy(authService);

        doThrow(new ServiceUnavailableException("Card Management service unavailable"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(SERVICE_UNAVAILABLE.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenInvalidRequest() {
        AuthService spyService = spy(authService);

        doThrow(new InvalidRollbackRequestException("Invalid rollback request"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(TRANSACTION_NOT_FOUND.reason());
    }

    @Test
    void rollbackShouldReturnDeclinedWhenUnexpectedError() {
        AuthService spyService = spy(authService);

        doThrow(new RuntimeException("Unexpected error"))
                .when(spyService).rollbackCard(any(RollbackRequest.class));

        RollbackResponse response = spyService.rollback(rollbackRequest, Instant.now());

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.declineReason()).isEqualTo(UNKNOWN_REASON.reason());
    }
}
