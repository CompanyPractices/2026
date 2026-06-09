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

import java.time.LocalDateTime;

import static com.processing.authorization.constants.DeclineOutcome.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private static final String TEST_RRN = "615514053700";
    private static final String TEST_AUTH_CODE = "A1B2C3";

    @BeforeEach
    void setUp() {
        validRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                5000L,
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
}
