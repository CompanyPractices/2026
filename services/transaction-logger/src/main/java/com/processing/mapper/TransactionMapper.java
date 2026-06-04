package com.processing.mapper;

import com.processing.dto.TransactionRequest;
import com.processing.dto.TransactionResponse;
import com.processing.dto.TransactionStoredResponse;
import com.processing.model.Transaction;
import org.springframework.stereotype.Component;

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
}
