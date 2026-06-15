package com.processing.transactionlogger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InternalTransactionControllerIntegrationTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void storeReturnsCreatedAndPersistsTransaction() throws Exception {
        TransactionRequest request = transactionRequest();

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.status").value("stored"));

        Transaction saved = transactionRepository.findById(TRANSACTION_ID).orElseThrow();

        assertThat(saved.getMti()).isEqualTo(request.mti());
        assertThat(saved.getStan()).isEqualTo(request.stan());
        assertThat(saved.getRrn()).isEqualTo(request.rrn());
        assertThat(saved.getPan()).isEqualTo(request.pan());
        assertThat(saved.getProcessingCode()).isEqualTo(request.processingCode());
        assertThat(saved.getAmount()).isEqualByComparingTo(request.amount());
        assertThat(saved.getCurrencyCode()).isEqualTo(request.currencyCode());
        assertThat(saved.getTerminalId()).isEqualTo(request.terminalId());
        assertThat(saved.getTerminalType()).isEqualTo(request.terminalType());
        assertThat(saved.getMerchantId()).isEqualTo(request.merchantId());
        assertThat(saved.getMcc()).isEqualTo(request.mcc());
        assertThat(saved.getAcquirerId()).isEqualTo(request.acquirerId());
        assertThat(saved.getIssuerId()).isEqualTo(request.issuerId());
        assertThat(saved.getAcquiringFee()).isEqualByComparingTo(request.acquiringFee());
        assertThat(saved.getStatus()).isEqualTo(request.status());
        assertThat(saved.getDeclineReason()).isEqualTo(request.declineReason());
        assertThat(saved.getAuthCode()).isEqualTo(request.authCode());
        assertThat(saved.getProcessingTimeMs()).isEqualTo(request.processingTimeMs());
        assertThat(saved.getTransmissionDateTime()).isEqualTo(request.transmissionDateTime());
        assertThat(saved.getCreatedAt()).isEqualTo(request.createdAt());
    }

    @Test
    void storeReturnsExistingTransactionAndDoesNotCreateDuplicate() throws Exception {
        TransactionRequest request = transactionRequest();
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.stan").value(request.stan()))
                .andExpect(jsonPath("$.pan").value(request.pan()));

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void storeReturnsConflictWhenExistingTransactionDiffers() throws Exception {
        TransactionRequest request = transactionRequest();
        TransactionRequest conflictingRequest = new TransactionRequest(
                request.id(),
                request.mti(),
                request.stan(),
                request.rrn(),
                request.pan(),
                request.processingCode(),
                new BigDecimal("200000"),
                request.currencyCode(),
                request.terminalId(),
                request.terminalType(),
                request.merchantId(),
                request.mcc(),
                request.acquirerId(),
                request.issuerId(),
                request.acquiringFee(),
                request.status(),
                request.declineReason(),
                request.authCode(),
                request.processingTimeMs(),
                request.transmissionDateTime(),
                request.createdAt()
        );

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Transaction conflict"));

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void storeReturnsBadRequestForInvalidTransaction() throws Exception {
        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void storeReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body"));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void storeReturnsBadRequestForNegativeAmount() throws Exception {
        TransactionRequest request = transactionRequestWithAmount(new BigDecimal("-1"));

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("amount")));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void storeReturnsCreatedAndPersistsDeclinedTransaction() throws Exception {
        TransactionRequest request = declinedTransactionRequest();

        mockMvc.perform(post("/api/internal/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.status").value("stored"));

        Transaction saved = transactionRepository.findById(TRANSACTION_ID).orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(saved.getDeclineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(saved.getRrn()).isNull();
        assertThat(saved.getAuthCode()).isNull();
        assertThat(saved.getAmount()).isEqualByComparingTo(request.amount());
        assertThat(saved.getPan()).isEqualTo(request.pan());
    }

    private static TransactionRequest transactionRequest() {
        return transactionRequestWithAmount(new BigDecimal("150000"));
    }

    private static TransactionRequest transactionRequestWithAmount(BigDecimal amount) {
        return new TransactionRequest(
                TRANSACTION_ID,
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                amount,
                "643",
                "TERM001",
                "POS",
                "MERCH1234567890",
                "5411",
                "ACQ001",
                "ISS001",
                new BigDecimal("2250"),
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }

    private static TransactionRequest declinedTransactionRequest() {
        return new TransactionRequest(
                TRANSACTION_ID,
                "0100",
                "000002",
                null,
                "4000001234560002",
                "000000",
                new BigDecimal("999999999"),
                "643",
                "TERM001",
                "POS",
                "MERCH1234567890",
                "5411",
                "ACQ001",
                "ISS001",
                null,
                TransactionStatus.DECLINED,
                "INSUFFICIENT_FUNDS",
                null,
                37,
                Instant.parse("2026-06-01T10:31:00Z"),
                Instant.parse("2026-06-01T10:31:01Z")
        );
    }
}
