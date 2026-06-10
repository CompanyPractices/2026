package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    Map<AcquirerFeeRequest, Double> acquirerFeeMap;

    public void calculateFee(Merchant merchant, Long amount, String stan, String pan) {
        double acquiringFee = (double) amount * merchant.getAcquiringFee().doubleValue();

        acquirerFeeMap.put(new AcquirerFeeRequest(stan, pan), acquiringFee);
        log.info("Calculate acquirer fee: STAN: {} PAN: {} Acquirer fee: {}", stan, pan, acquiringFee);
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        try {
            double acquirerFee = acquirerFeeMap.get(request);
            log.info("Request for get acquirer fee: STAN: {} PAN: {} Acquirer fee: {}",
                    request.stan(), request.pan(), acquirerFee);

            return new AcquirerFeeResponse(request.stan(), request.pan(), acquirerFee);
        } catch (NullPointerException e) {
            throw new NullPointerException("Local table with acquirerFee is empty. Try generate transactions");
        }
    }
}
