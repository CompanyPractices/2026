package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.repository.InMemoryAcquirerFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class AcquirerProviderTest {
    private AcquirerProvider acquirerProvider;
    private InMemoryAcquirerFeeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAcquirerFeeRepository();
        acquirerProvider = new AcquirerProvider(repository);
    }

    @Test
    void calculateFee(){
        Merchant merchant = Mockito.mock(Merchant.class);
        when(merchant.getAcquiringFee()).thenReturn(new BigDecimal("0.067"));

        long amount = 139_999;
        String pan = "4444445808467586";
        String stan = "000004";
        String terminalId = "TERM001";
        double expected = amount * 0.067;

        acquirerProvider.calculateFee(merchant, amount, stan, pan, terminalId);

        AcquirerFeeRequest expectedRequest = new AcquirerFeeRequest(stan, pan, terminalId);
        assertEquals(expected, repository.get(expectedRequest));
    }

    @Test
    void getFee(){
        int amount = 139_999;
        String pan = "4444445808467586";
        String stan = "000004";
        String terminalId = "TERM001";
        double fee = amount * 0.067;

        AcquirerFeeRequest acquirerFeeRequest = new AcquirerFeeRequest(stan, pan, terminalId);
        repository.save(acquirerFeeRequest, fee);

        AcquirerFeeResponse acquirerFeeResponse = acquirerProvider.getAcquirerFee(acquirerFeeRequest);

        assertEquals(pan, acquirerFeeResponse.pan());
        assertEquals(stan, acquirerFeeResponse.stan());
        assertEquals(fee, acquirerFeeResponse.acquirerFee());
    }

    @Test
    void getUnrealFeeInMap() {
        String pan = "4444445808467586";
        String stan = "000004";
        String terminalId = "TERM001";
        AcquirerFeeRequest acquirerFeeRequest = new AcquirerFeeRequest(stan, pan, terminalId);

        assertThrows(NullPointerException.class,
                () -> acquirerProvider.getAcquirerFee(acquirerFeeRequest));
    }
}
