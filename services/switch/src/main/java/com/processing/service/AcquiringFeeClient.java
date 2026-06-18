package com.processing.service;

import java.math.BigDecimal;
import java.time.Instant;

public interface AcquiringFeeClient {

    BigDecimal fetchAcquiringFee(Instant transmissionDateTime, String stan, String pan, String terminalId, BigDecimal amount);
}
