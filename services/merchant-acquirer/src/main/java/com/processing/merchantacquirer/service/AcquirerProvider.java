package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    private final AcquirerFeeRepository repository;
    private final MerchantProvider merchantProvider;

    public void calculateFee(
            String merchantId, Long amount, Instant transmissionDateTime, String stan, String terminalId, String pan) {
        Long fee = merchantProvider.getMerchantAcquirerFee(merchantId);
        Long acquiringFee = amount * fee / 1000;

        AcquirerFee acquirerFeeEntity = new AcquirerFee(transmissionDateTime, stan, pan, terminalId, acquiringFee, amount);
        repository.save(acquirerFeeEntity);
        log.info("Calculate acquirer fee: {}",
                acquirerFeeEntity);
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        Long acquirerFee = repository.findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
                request.transmissionDateTime(), request.stan(), request.terminalId(),
                request.amount(), request.pan()).getAcquirerFee();
        log.info("Request for get acquirer fee: DataTime: {} STAN: {} Acquirer fee: {}",
                request.transmissionDateTime(), request.stan(), acquirerFee);

        return new AcquirerFeeResponse(acquirerFee);
    }
}
