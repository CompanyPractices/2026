package com.processing.service;

public interface AcquiringFeeClient {

    Long fetchAcquiringFee(String stan, String pan, String terminalId);
}
