package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    private final AcquirerFeeRepository repository;
    private final MerchantProvider merchantProvider;

    public void calculateFee(
            String merchantId, BigDecimal amount, Instant transmissionDateTime, String stan, String terminalId, String pan) {
        BigDecimal fee = merchantProvider.getMerchantAcquirerFee(merchantId);
        BigDecimal acquiringFee = amount
                .multiply(fee)
                .setScale(0, RoundingMode.HALF_EVEN);

        AcquirerFee acquirerFeeEntity = new AcquirerFee(transmissionDateTime, stan, pan, terminalId, acquiringFee, amount);
        repository.save(acquirerFeeEntity);
        log.info("Calculate Acquirer fee: {}",
                acquirerFeeEntity);
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        BigDecimal acquirerFee = repository.findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
                request.transmissionDateTime(), request.stan(), request.terminalId(),
                request.amount(), request.pan()).getAcquirerFee();
        log.info("Request for get acquirer fee: DataTime: {} STAN: {} Acquirer fee: {}",
                request.transmissionDateTime(), request.stan(), acquirerFee);

        return new AcquirerFeeResponse(acquirerFee);
    }
}
