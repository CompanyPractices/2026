package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class AcquirerProviderTest {
    private AcquirerProvider acquirerProvider;
    private AcquirerFeeRepository repository;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AcquirerFeeRepository.class);
        acquirerProvider = new AcquirerProvider(repository);
    }

    @Test
    void getFee(){
        BigDecimal amount = BigDecimal.valueOf(139_999);
        String stan = "000004";
        String transmissionDateTime = "2026-06-10T18:19:25.843989500";
        String terminalId = "TERM001";
        String pan = "4000005310852539";
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(67)).multiply(BigDecimal.valueOf(0.001));

        AcquirerFeeRequest acquirerFeeRequest = new AcquirerFeeRequest(transmissionDateTime, pan, stan, amount, terminalId);
        when(repository.findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(transmissionDateTime, stan, terminalId, amount, pan)).thenReturn(new AcquirerFee(transmissionDateTime, stan, pan, terminalId, fee, amount));

        AcquirerFeeResponse acquirerFeeResponse = acquirerProvider.getAcquirerFee(acquirerFeeRequest);

        assertEquals(fee, acquirerFeeResponse.acquirerFee());
    }

    @Test
    void getUnrealFeeInMap() {
        String transmissionDateTime = "2026-06-10T18:19:25.843989500";
        String stan = "000004";
        String terminalId = "TERM001";
        AcquirerFeeRequest acquirerFeeRequest = new AcquirerFeeRequest(transmissionDateTime, null, stan, null, terminalId);

        assertThrows(ResourceNotFoundException.class,
                () -> acquirerProvider.getAcquirerFee(acquirerFeeRequest));
    }
}
