package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AcquirerProviderTest {
    private AcquirerProvider acquirerProvider;
    private MerchantProvider merchantProvider;
    private AcquirerFeeRepository repository;

    @BeforeEach
    void setUp() {
        merchantProvider = Mockito.mock(MerchantProvider.class);
        repository = Mockito.mock(AcquirerFeeRepository.class);
        acquirerProvider = new AcquirerProvider(repository);
    }

    @Test
    void calculateFee(){
        Merchant merchant = new Merchant(
                "MERCH00000000007",
                "Ашан Сити",
                "5411",
                "grocery",
                "ACQ003",
                BigDecimal.valueOf(0.015),
                BigDecimal.valueOf(145000));

        when(merchantProvider.getMerchantAcquirerFee(merchant.getAcquirerId())).thenReturn(BigDecimal.valueOf(0.015));

        BigDecimal amount = BigDecimal.valueOf(139999);
        String stan = "000004";
        String rrn = "616113423602";
        String terminalId = "TERM001";
        String pan = "4000005310852539";
        BigDecimal expected = amount
                .multiply(BigDecimal.valueOf(0.015))
                .setScale(0, RoundingMode.HALF_EVEN);

        acquirerProvider.calculateFee(merchant.getAcquiringFee(), amount, rrn, stan, terminalId, pan);
        ArgumentCaptor<AcquirerFee> feeCaptor = ArgumentCaptor.forClass(AcquirerFee.class);

        verify(repository).save(feeCaptor.capture());
        AcquirerFee capturedFee = feeCaptor.getValue();
        assertEquals(expected, capturedFee.getAcquirerFee());
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

        assertThrows(NullPointerException.class,
                () -> acquirerProvider.getAcquirerFee(acquirerFeeRequest));
    }
}
