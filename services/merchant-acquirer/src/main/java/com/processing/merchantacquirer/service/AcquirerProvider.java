package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    public final AcquirerFeeRepository repository;
    private final MerchantProvider merchantProvider;

    public void calculateFee(String merchantId, Long amount, String transmissionDateTime, String stan, String terminalId) {
        Long fee = merchantProvider.getMerchantAcquirerFee(merchantId);
        Long acquiringFee = amount * fee / 1000;

        repository.save(
                new AcquirerFee(transmissionDateTime, stan, terminalId, acquiringFee));
        log.info("Calculate acquirer fee: DataTime: {} STAN: {} Acquirer fee: {} Amount: {} Fee: {} TerminalID: {}",
                transmissionDateTime, stan, acquiringFee, amount, fee, terminalId);
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        Long acquirerFee = repository.findByTransmissionDateTimeAndStanAndTerminalId(
                request.transmissionDateTime(), request.stan(), request.terminalId()).getAcquirerFee();
        log.info("Request for get acquirer fee: DataTime: {} STAN: {} Acquirer fee: {}",
                request.transmissionDateTime(), request.stan(), acquirerFee);

        return new AcquirerFeeResponse(request.transmissionDateTime(), request.stan(), acquirerFee);
    }
}
