package com.processing.transactionlogger.mapper;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.model.Transaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private static final Set<String> MATCHED_REQUEST_FIELDS = Set.of(
            "id",
            "mti",
            "stan",
            "rrn",
            "pan",
            "processingCode",
            "amount",
            "currencyCode",
            "terminalId",
            "terminalType",
            "merchantId",
            "mcc",
            "acquirerId",
            "issuerId",
            "acquiringFee",
            "status",
            "declineReason",
            "authCode",
            "processingTimeMs",
            "transmissionDateTime"
    );
    private static final Set<String> IGNORED_REQUEST_FIELDS_FOR_MATCHES = Set.of("createdAt");

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void toEntityCopiesAllRequestFields() {
        TransactionRequest request = transactionRequest();

        Transaction transaction = mapper.toEntity(request);

        assertThat(transaction.getId()).isEqualTo(request.id());
        assertThat(transaction.getMti()).isEqualTo(request.mti());
        assertThat(transaction.getStan()).isEqualTo(request.stan());
        assertThat(transaction.getRrn()).isEqualTo(request.rrn());
        assertThat(transaction.getPan()).isEqualTo(request.pan());
        assertThat(transaction.getProcessingCode()).isEqualTo(request.processingCode());
        assertThat(transaction.getAmount()).isEqualTo(request.amount());
        assertThat(transaction.getCurrencyCode()).isEqualTo(request.currencyCode());
        assertThat(transaction.getTerminalId()).isEqualTo(request.terminalId());
        assertThat(transaction.getTerminalType()).isEqualTo(request.terminalType());
        assertThat(transaction.getMerchantId()).isEqualTo(request.merchantId());
        assertThat(transaction.getMcc()).isEqualTo(request.mcc());
        assertThat(transaction.getAcquirerId()).isEqualTo(request.acquirerId());
        assertThat(transaction.getIssuerId()).isEqualTo(request.issuerId());
        assertThat(transaction.getAcquiringFee()).isEqualTo(request.acquiringFee());
        assertThat(transaction.getStatus()).isEqualTo(request.status());
        assertThat(transaction.getDeclineReason()).isEqualTo(request.declineReason());
        assertThat(transaction.getAuthCode()).isEqualTo(request.authCode());
        assertThat(transaction.getProcessingTimeMs()).isEqualTo(request.processingTimeMs());
        assertThat(transaction.getTransmissionDateTime()).isEqualTo(request.transmissionDateTime());
        assertThat(transaction.getCreatedAt()).isEqualTo(request.createdAt());
    }

    @Test
    void toResponseCopiesAllTransactionFields() {
        Transaction transaction = transaction();

        TransactionResponse response = mapper.toResponse(transaction);

        assertThat(response.id()).isEqualTo(transaction.getId());
        assertThat(response.mti()).isEqualTo(transaction.getMti());
        assertThat(response.stan()).isEqualTo(transaction.getStan());
        assertThat(response.rrn()).isEqualTo(transaction.getRrn());
        assertThat(response.pan()).isEqualTo(transaction.getPan());
        assertThat(response.processingCode()).isEqualTo(transaction.getProcessingCode());
        assertThat(response.amount()).isEqualTo(transaction.getAmount());
        assertThat(response.currencyCode()).isEqualTo(transaction.getCurrencyCode());
        assertThat(response.terminalId()).isEqualTo(transaction.getTerminalId());
        assertThat(response.terminalType()).isEqualTo(transaction.getTerminalType());
        assertThat(response.merchantId()).isEqualTo(transaction.getMerchantId());
        assertThat(response.mcc()).isEqualTo(transaction.getMcc());
        assertThat(response.acquirerId()).isEqualTo(transaction.getAcquirerId());
        assertThat(response.issuerId()).isEqualTo(transaction.getIssuerId());
        assertThat(response.acquiringFee()).isEqualTo(transaction.getAcquiringFee());
        assertThat(response.status()).isEqualTo(transaction.getStatus());
        assertThat(response.declineReason()).isEqualTo(transaction.getDeclineReason());
        assertThat(response.authCode()).isEqualTo(transaction.getAuthCode());
        assertThat(response.processingTimeMs()).isEqualTo(transaction.getProcessingTimeMs());
        assertThat(response.transmissionDateTime()).isEqualTo(transaction.getTransmissionDateTime());
        assertThat(response.createdAt()).isEqualTo(transaction.getCreatedAt());
    }

    @Test
    void toStoredResponseReturnsStoredStatus() {
        Transaction transaction = transaction();

        TransactionStoredResponse response = mapper.toStoredResponse(transaction);

        assertThat(response.id()).isEqualTo(transaction.getId());
        assertThat(response.status()).isEqualTo("stored");
    }

    @Test
    void matchesReturnsTrueWhenTransactionMatchesRequest() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction();

        boolean matches = mapper.matches(transaction, request);

        assertThat(matches).isTrue();
    }

    @Test
    void matchesReturnsFalseWhenTransactionDiffersFromRequest() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction();
        transaction.setAmount(new BigDecimal("200000"));

        boolean matches = mapper.matches(transaction, request);

        assertThat(matches).isFalse();
    }

    @Test
    void matchesReturnsFalseWhenTerminalTypeDiffersFromRequest() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction();
        transaction.setTerminalType("ATM");

        boolean matches = mapper.matches(transaction, request);

        assertThat(matches).isFalse();
    }

    @Test
    void matchesReturnsTrueWhenOnlyCreatedAtDiffers() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction();
        transaction.setCreatedAt(Instant.parse("2026-06-01T10:31:01Z"));

        boolean matches = mapper.matches(transaction, request);

        assertThat(matches).isTrue();
    }

    @Test
    void matchesCoverageAccountsForAllTransactionRequestFields() {
        Set<String> requestFields = Arrays.stream(TransactionRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> accountedFields = java.util.stream.Stream.concat(
                        MATCHED_REQUEST_FIELDS.stream(),
                        IGNORED_REQUEST_FIELDS_FOR_MATCHES.stream()
                )
                .collect(Collectors.toUnmodifiableSet());

        assertThat(MATCHED_REQUEST_FIELDS).doesNotContain("createdAt");
        assertThat(IGNORED_REQUEST_FIELDS_FOR_MATCHES).containsExactly("createdAt");
        assertThat(MATCHED_REQUEST_FIELDS).doesNotContainAnyElementsOf(IGNORED_REQUEST_FIELDS_FOR_MATCHES);
        assertThat(accountedFields).isEqualTo(requestFields);
    }

    private static TransactionRequest transactionRequest() {
        return new TransactionRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                new BigDecimal("150000"),
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

    private static Transaction transaction() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = new Transaction();
        transaction.setId(request.id());
        transaction.setMti(request.mti());
        transaction.setStan(request.stan());
        transaction.setRrn(request.rrn());
        transaction.setPan(request.pan());
        transaction.setProcessingCode(request.processingCode());
        transaction.setAmount(request.amount());
        transaction.setCurrencyCode(request.currencyCode());
        transaction.setTerminalId(request.terminalId());
        transaction.setTerminalType(request.terminalType());
        transaction.setMerchantId(request.merchantId());
        transaction.setMcc(request.mcc());
        transaction.setAcquirerId(request.acquirerId());
        transaction.setIssuerId(request.issuerId());
        transaction.setAcquiringFee(request.acquiringFee());
        transaction.setStatus(request.status());
        transaction.setDeclineReason(request.declineReason());
        transaction.setAuthCode(request.authCode());
        transaction.setProcessingTimeMs(request.processingTimeMs());
        transaction.setTransmissionDateTime(request.transmissionDateTime());
        transaction.setCreatedAt(request.createdAt());
        return transaction;
    }
}
