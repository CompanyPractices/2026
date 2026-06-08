package com.processing.mapper;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TransactionMapper {

    private static final String STORED_STATUS = "stored";

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

    public boolean matches(Transaction transaction, TransactionRequest request) {
        return Objects.equals(transaction.getId(), request.id())
                && Objects.equals(transaction.getMti(), request.mti())
                && Objects.equals(transaction.getStan(), request.stan())
                && Objects.equals(transaction.getRrn(), request.rrn())
                && Objects.equals(transaction.getPan(), request.pan())
                && Objects.equals(transaction.getProcessingCode(), request.processingCode())
                && Objects.equals(transaction.getAmount(), request.amount())
                && Objects.equals(transaction.getCurrencyCode(), request.currencyCode())
                && Objects.equals(transaction.getTerminalId(), request.terminalId())
                && Objects.equals(transaction.getMerchantId(), request.merchantId())
                && Objects.equals(transaction.getMcc(), request.mcc())
                && Objects.equals(transaction.getAcquirerId(), request.acquirerId())
                && Objects.equals(transaction.getIssuerId(), request.issuerId())
                && Objects.equals(transaction.getAcquiringFee(), request.acquiringFee())
                && Objects.equals(transaction.getStatus(), request.status())
                && Objects.equals(transaction.getDeclineReason(), request.declineReason())
                && Objects.equals(transaction.getAuthCode(), request.authCode())
                && Objects.equals(transaction.getProcessingTimeMs(), request.processingTimeMs())
                && Objects.equals(transaction.getTransmissionDateTime(), request.transmissionDateTime())
                && Objects.equals(transaction.getCreatedAt(), request.createdAt());
    }
}
