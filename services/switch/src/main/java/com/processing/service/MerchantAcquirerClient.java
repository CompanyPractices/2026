package com.processing.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.processing.config.SwitchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MerchantAcquirerClient implements AcquiringFeeClient {

    private static final Logger LOG = LoggerFactory.getLogger(MerchantAcquirerClient.class);

    private final SwitchProperties switchProperties;
    private final RestClient restClient;

    public MerchantAcquirerClient(SwitchProperties switchProperties, RestClient restClient) {
        this.switchProperties = switchProperties;
        this.restClient = restClient;
    }

    @Override
    public Long fetchAcquiringFee(String stan, String pan, String terminalId) {
        try {
            AcquirerFeeResponse response = restClient.method(HttpMethod.GET)
                    .uri(switchProperties.merchantAcquirerUrl() + "/api/simulator/merchant/fee")
                    .body(new AcquirerFeeRequest(stan, pan, terminalId))
                    .retrieve()
                    .body(AcquirerFeeResponse.class);
            if (response == null) {
                return null;
            }
            return Math.round(response.acquirerFee());
        } catch (Exception e) {
            LOG.warn("Acquiring fee unavailable for STAN={}: {}", stan, e.getMessage());
            return null;
        }
    }

    private record AcquirerFeeRequest(
            String stan,
            String pan,
            @JsonProperty("terminalID") String terminalId
    ) {
    }

    private record AcquirerFeeResponse(
            String stan,
            String pan,
            double acquirerFee
    ) {
    }
}
