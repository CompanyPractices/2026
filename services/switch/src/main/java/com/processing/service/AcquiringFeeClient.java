package com.processing.service;

public interface AcquiringFeeClient {

    Long fetchAcquiringFee(String transmissionDateTime, String stan, String pan, String terminalId, Long amount);
}
