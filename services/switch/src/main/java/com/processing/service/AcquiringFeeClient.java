package com.processing.service;

import java.math.BigDecimal;

public interface AcquiringFeeClient {

    BigDecimal fetchAcquiringFee(String transmissionDateTime, String stan, String pan, String terminalId, BigDecimal amount);
}
