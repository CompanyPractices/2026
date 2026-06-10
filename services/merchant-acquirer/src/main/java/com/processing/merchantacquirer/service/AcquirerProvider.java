package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    public final AcquirerFeeRepository repository;

    public void calculateFee(Merchant merchant, Long amount, String stan, String pan, String terminalId) {
        BigDecimal acquiringFee = new BigDecimal(((double) amount / 10) * merchant.getAcquiringFee().doubleValue());

        repository.save(new AcquirerFeeRequest(stan, pan, terminalId), acquiringFee);
        log.info("Calculate acquirer fee: STAN: {} PAN: {} Acquirer fee: {} TerminalID: {}", stan, pan, acquiringFee, terminalId);
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        BigDecimal acquirerFee = repository.get(request);
        log.info("Request for get acquirer fee: STAN: {} PAN: {} Acquirer fee: {}",
                request.stan(), request.pan(), acquirerFee);

        return new AcquirerFeeResponse(request.stan(), request.pan(), acquirerFee);
    }
}
