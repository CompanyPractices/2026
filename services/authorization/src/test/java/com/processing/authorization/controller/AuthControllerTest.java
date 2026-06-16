package com.processing.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.authorization.*;
import com.processing.authorization.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static com.processing.authorization.constants.DeclineOutcome.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import java.math.BigDecimal;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthorizationRequest validRequest;
    private AuthorizationResponse approvedResponse;
    private AuthorizationResponse declinedInsufficientFundsResponse;
    private AuthorizationResponse declinedCardExpiredResponse;
    private AuthorizationResponse declinedCardNotFoundResponse;
    private AuthorizationResponse declinedServiceUnavailableResponse;

    private RollbackRequest validRollbackRequest;
    private RollbackResponse rollbackApprovedResponse;
    private RollbackResponse rollbackDeclinedResponse;
    private RollbackResponse rollbackNotFoundResponse;
    private RollbackResponse rollbackConflictResponse;
    private RollbackResponse rollbackServiceUnavailableResponse;

    private static final String TEST_RRN = "615514053700";
    private static final String TEST_AUTH_CODE = "A1B2C3";
    private static final String TEST_PAN = "1234567890123456";

    @BeforeEach
    void setUp() {
        validRequest = new AuthorizationRequest(
                "0100",
                "123456",
                TEST_PAN,
                "000000",
                BigDecimal.valueOf(5000),
                "810",
                "2026-06-05T18:12:49.07",
                "T0000001",
                null,
                "M00000000000001",
                "5411",
                "A001",
                "I001"
        );

        approvedResponse = AuthorizationResponse.approved(
                validRequest, TEST_RRN,
                TEST_AUTH_CODE, LocalDateTime.now()
        );
        declinedInsufficientFundsResponse = AuthorizationResponse.declined(
                validRequest, INSUFFICIENT_FUNDS.reason(),
                INSUFFICIENT_FUNDS.code(), LocalDateTime.now()
        );
        declinedCardExpiredResponse = AuthorizationResponse.declined(
                validRequest, CARD_EXPIRED.reason(),
                CARD_EXPIRED.code(), LocalDateTime.now()
        );
        declinedCardNotFoundResponse = AuthorizationResponse.declined(
                validRequest, CARD_NOT_FOUND.reason(),
                CARD_NOT_FOUND.code(), LocalDateTime.now()
        );
        declinedServiceUnavailableResponse = AuthorizationResponse.declined(
                validRequest, SERVICE_UNAVAILABLE.reason(),
                SERVICE_UNAVAILABLE.code(), LocalDateTime.now()
        );

        validRollbackRequest = new RollbackRequest(
                TEST_RRN,
                TEST_PAN,
                new BigDecimal(5000)
        );

        rollbackApprovedResponse = RollbackResponse.approved(
                TEST_RRN,
                Instant.now()
        );
        rollbackDeclinedResponse = RollbackResponse.declined(
                TEST_RRN,
                ROLLBACK_FAILED.reason(),
                ROLLBACK_FAILED.code(),
                Instant.now()
        );
        rollbackNotFoundResponse = RollbackResponse.declined(
                TEST_RRN,
                TRANSACTION_NOT_FOUND.reason(),
                TRANSACTION_NOT_FOUND.code(),
                Instant.now()
        );
        rollbackConflictResponse = RollbackResponse.declined(
                TEST_RRN,
                ALREADY_ROLLED_BACK.reason(),
                ALREADY_ROLLED_BACK.code(),
                Instant.now()
        );
        rollbackServiceUnavailableResponse = RollbackResponse.declined(
                TEST_RRN,
                SERVICE_UNAVAILABLE.reason(),
                SERVICE_UNAVAILABLE.code(),
                Instant.now()
        );
    }

    @Test
    void authorizeReturn200ApprovedResponseWhenAuthServiceReturnsApproved() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class), any(LocalDateTime.class))).thenReturn(approvedResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(AuthorizationResponse.CODE_APPROVED))
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_APPROVED))
                .andExpect(jsonPath("$.rrn").value(TEST_RRN))
                .andExpect(jsonPath("$.authCode").value(TEST_AUTH_CODE))
                .andExpect(jsonPath("$.declineReason").doesNotExist());
    }

    @Test
    void authorizeReturn422WhenInsufficientFunds() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class), any(LocalDateTime.class))).thenReturn(declinedInsufficientFundsResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(INSUFFICIENT_FUNDS.code()))
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.declineReason").value(INSUFFICIENT_FUNDS.reason()));
    }

    @Test
    void authorizeReturn403WhenCardExpired() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class), any(LocalDateTime.class))).thenReturn(declinedCardExpiredResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(CARD_EXPIRED.code()))
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.declineReason").value(CARD_EXPIRED.reason()));
    }

    @Test
    void authorizeReturn404WhenCardNotFound() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class), any(LocalDateTime.class))).thenReturn(declinedCardNotFoundResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(CARD_NOT_FOUND.code()))
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.declineReason").value(CARD_NOT_FOUND.reason()));
    }

    @Test
    void authorizeReturn503WhenServiceUnavailable() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class), any(LocalDateTime.class))).thenReturn(declinedServiceUnavailableResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(SERVICE_UNAVAILABLE.code()))
                .andExpect(jsonPath("$.status").value(AuthorizationResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.declineReason").value(SERVICE_UNAVAILABLE.reason()));
    }

    @Test
    void authorizeReturn400WhenRequestIsInvalid() throws Exception {
        String invalidJson = "{\"pan\":\"123\"}";

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rollbackReturn200WhenRollbackApproved() throws Exception {
        when(authService.rollback(any(RollbackRequest.class), any(LocalDateTime.class)))
                .thenReturn(rollbackApprovedResponse);

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRollbackRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(RollbackResponse.STATUS_APPROVED))
                .andExpect(jsonPath("$.responseCode").value(RollbackResponse.CODE_SUCCESS))
                .andExpect(jsonPath("$.rrn").value(TEST_RRN))
                .andExpect(jsonPath("$.declineReason").doesNotExist());
    }

    @Test
    void rollbackReturn404WhenTransactionNotFound() throws Exception {
        when(authService.rollback(any(RollbackRequest.class), any(LocalDateTime.class)))
                .thenReturn(rollbackNotFoundResponse);

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRollbackRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(RollbackResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.responseCode").value(TRANSACTION_NOT_FOUND.code()))
                .andExpect(jsonPath("$.declineReason").value(TRANSACTION_NOT_FOUND.reason()));
    }

    @Test
    void rollbackReturn409WhenAlreadyRolledBack() throws Exception {
        when(authService.rollback(any(RollbackRequest.class), any(LocalDateTime.class)))
                .thenReturn(rollbackConflictResponse);

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRollbackRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(RollbackResponse.STATUS_DECLINED))
                .andExpect(jsonPath("$.responseCode").value(ALREADY_ROLLED_BACK.code()))
                .andExpect(jsonPath("$.declineReason").value(ALREADY_ROLLED_BACK.reason()));
    }

    @Test
    void rollbackReturn503WhenServiceUnavailable() throws Exception {
        when(authService.rollback(any(RollbackRequest.class), any(LocalDateTime.class)))
                .thenReturn(rollbackServiceUnavailableResponse);

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRollbackRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value(SERVICE_UNAVAILABLE.code()))
                .andExpect(jsonPath("$.declineReason").value(SERVICE_UNAVAILABLE.reason()));
    }

    @Test
    void rollbackReturn400WhenRequestIsInvalid() throws Exception {
        String invalidJson = "{\"rrn\":\"\"}";

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rollbackReturn400WhenAmountIsNegative() throws Exception {
        String invalidJson = """
                {
                  "rrn": "012345678901",
                  "amount": -100
                }
                """;

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rollbackReturn400WhenRrnIsMissing() throws Exception {
        String invalidJson = """
                {
                  "amount": 5000
                }
                """;

        mockMvc.perform(post("/api/internal/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
