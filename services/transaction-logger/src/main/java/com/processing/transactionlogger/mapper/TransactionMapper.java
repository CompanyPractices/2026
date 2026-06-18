package com.processing.transactionlogger.mapper;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.transactionlogger.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Конвертирует объекты транзакций между слоями: DTO ↔ Entity ↔ Response.
 */
@Component
public class TransactionMapper {

    private static final String STORED_STATUS = "stored";
    private static final Set<String> IGNORED_REQUEST_FIELDS_FOR_MATCHES = Set.of("createdAt");
    private static final Map<String, BiPredicate<Transaction, TransactionRequest>> REQUEST_FIELD_MATCHERS =
            Map.ofEntries(
                    Map.entry("id", (transaction, request) -> Objects.equals(transaction.getId(), request.id())),
                    Map.entry("mti", (transaction, request) -> Objects.equals(transaction.getMti(), request.mti())),
                    Map.entry("stan", (transaction, request) -> Objects.equals(transaction.getStan(), request.stan())),
                    Map.entry("rrn", (transaction, request) -> Objects.equals(transaction.getRrn(), request.rrn())),
                    Map.entry("pan", (transaction, request) -> Objects.equals(transaction.getPan(), request.pan())),
                    Map.entry("processingCode", (transaction, request) ->
                            Objects.equals(transaction.getProcessingCode(), request.processingCode())),
                    Map.entry("amount", (transaction, request) ->
                            compareBigDecimal(transaction.getAmount(), request.amount())),
                    Map.entry("currencyCode", (transaction, request) ->
                            Objects.equals(transaction.getCurrencyCode(), request.currencyCode())),
                    Map.entry("terminalId", (transaction, request) ->
                            Objects.equals(transaction.getTerminalId(), request.terminalId())),
                    Map.entry("terminalType", (transaction, request) ->
                            Objects.equals(transaction.getTerminalType(), request.terminalType())),
                    Map.entry("merchantId", (transaction, request) ->
                            Objects.equals(transaction.getMerchantId(), request.merchantId())),
                    Map.entry("mcc", (transaction, request) -> Objects.equals(transaction.getMcc(), request.mcc())),
                    Map.entry("acquirerId", (transaction, request) ->
                            Objects.equals(transaction.getAcquirerId(), request.acquirerId())),
                    Map.entry("issuerId", (transaction, request) ->
                            Objects.equals(transaction.getIssuerId(), request.issuerId())),
                    Map.entry("acquiringFee", (transaction, request) ->
                            compareBigDecimal(transaction.getAcquiringFee(), request.acquiringFee())),
                    Map.entry("status", (transaction, request) ->
                            Objects.equals(transaction.getStatus(), request.status())),
                    Map.entry("declineReason", (transaction, request) ->
                            Objects.equals(transaction.getDeclineReason(), request.declineReason())),
                    Map.entry("authCode", (transaction, request) ->
                            Objects.equals(transaction.getAuthCode(), request.authCode())),
                    Map.entry("processingTimeMs", (transaction, request) ->
                            Objects.equals(transaction.getProcessingTimeMs(), request.processingTimeMs())),
                    Map.entry("transmissionDateTime", (transaction, request) ->
                            Objects.equals(transaction.getTransmissionDateTime(), request.transmissionDateTime()))
            );

    public Transaction toEntity(TransactionRequest request) {
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

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getMti(),
                transaction.getStan(),
                transaction.getRrn(),
                transaction.getPan(),
                transaction.getProcessingCode(),
                transaction.getAmount(),
                transaction.getCurrencyCode(),
                transaction.getTerminalId(),
                transaction.getTerminalType(),
                transaction.getMerchantId(),
                transaction.getMcc(),
                transaction.getAcquirerId(),
                transaction.getIssuerId(),
                transaction.getAcquiringFee(),
                transaction.getStatus(),
                transaction.getDeclineReason(),
                transaction.getAuthCode(),
                transaction.getProcessingTimeMs(),
                transaction.getTransmissionDateTime(),
                transaction.getCreatedAt()
        );
    }

    public TransactionStoredResponse toStoredResponse(Transaction transaction) {
        return new TransactionStoredResponse(transaction.getId(), STORED_STATUS);
    }

    /**
     * Проверяет, совпадают ли поля существующей транзакции с входящим запросом.
     * Используется для идемпотентности: повторный запрос с теми же бизнес-данными не является конфликтом.
     * Поле {@code createdAt} намеренно не участвует в сравнении.
     *
     * @param transaction запись из БД
     * @param request     входящий запрос от Switch
     * @return {@code true} если все поля идентичны
     */
    public boolean matches(Transaction transaction, TransactionRequest request) {
        return REQUEST_FIELD_MATCHERS.values().stream()
                .allMatch(matcher -> matcher.test(transaction, request));
    }

    static Set<String> matchedRequestFields() {
        return REQUEST_FIELD_MATCHERS.keySet();
    }

    static Set<String> ignoredRequestFieldsForMatches() {
        return IGNORED_REQUEST_FIELDS_FOR_MATCHES;
    }

    private static boolean compareBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }
}
