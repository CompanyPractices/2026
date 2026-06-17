package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    private final AcquirerFeeRepository repository;

    public void saveAll(List<AcquirerFee> fees) {
        repository.saveAll(fees);
        log.info("Saved {} acquiring fees", fees.size());
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        BigDecimal acquirerFee = repository.findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
                request.transmissionDateTime(), request.stan(), request.terminalId(),
                request.amount(), request.pan()).getAcquirerFee();
        if (acquirerFee == null) {
            log.warn("AcquirerFee not found for request: TransmissiontDataTime: {}, STAN: {}, TerminalId: {}",
                    request.transmissionDateTime(), request.stan(), request.terminalId());
            throw new ResourceNotFoundException("Acquirer fee not found for stan = " + request.stan());
        }
        log.info("Request for get acquirer fee: DataTime: {} STAN: {} Acquirer fee: {}",
                request.transmissionDateTime(), request.stan(), acquirerFee);

        return new AcquirerFeeResponse(acquirerFee);
    }
}
