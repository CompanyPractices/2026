package com.processing.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.authorization.dto.AuthorizationRequest;
import com.processing.authorization.dto.AuthorizationResponse;
import com.processing.authorization.enums.TerminalType;
import com.processing.authorization.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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

    @BeforeEach
    void setUp() {
        validRequest = new AuthorizationRequest(
                "0100",
                "123456",
                "1234567890123456",
                "000000",
                5000,
                "810",
                LocalDateTime.now(),
                "T0000001",
                TerminalType.POS,
                "M00000000000001",
                "5411",
                "A001",
                "I001"
        );

        approvedResponse = AuthorizationResponse.approved(validRequest, "615514053700", "A1B2C3");
        declinedInsufficientFundsResponse = AuthorizationResponse.declined(validRequest, "INSUFFICIENT_FUNDS", "51");
        declinedCardExpiredResponse = AuthorizationResponse.declined(validRequest, "CARD_EXPIRED", "54");
        declinedCardNotFoundResponse = AuthorizationResponse.declined(validRequest, "CARD_NOT_FOUND", "14");
        declinedServiceUnavailableResponse = AuthorizationResponse.declined(validRequest, "SERVICE_UNAVAILABLE", "96");
    }

    @Test
    void authorizeReturn200ApprovedResponseWhenAuthServiceReturnsApproved() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class))).thenReturn(approvedResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.rrn").value("615514053700"))
                .andExpect(jsonPath("$.authCode").value("A1B2C3"))
                .andExpect(jsonPath("$.declineReason").doesNotExist());
    }

    @Test
    void authorizeReturn422WhenInsufficientFunds() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class))).thenReturn(declinedInsufficientFundsResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value("51"))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void authorizeReturn403WhenCardExpired() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class))).thenReturn(declinedCardExpiredResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value("54"))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("CARD_EXPIRED"));
    }

    @Test
    void authorizeReturn404WhenCardNotFound() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class))).thenReturn(declinedCardNotFoundResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value("14"))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("CARD_NOT_FOUND"));
    }

    @Test
    void authorizeReturn503WhenServiceUnavailable() throws Exception {
        when(authService.authorize(any(AuthorizationRequest.class))).thenReturn(declinedServiceUnavailableResponse);

        mockMvc.perform(post("/api/internal/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.responseCode").value("96"))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("SERVICE_UNAVAILABLE"));
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