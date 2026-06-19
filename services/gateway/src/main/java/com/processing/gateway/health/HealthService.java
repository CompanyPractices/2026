package com.processing.gateway.health;

import com.processing.gateway.health.models.HealthRequest;
import com.processing.gateway.health.models.HealthResponse;
import com.processing.gateway.health.models.HealthStatus;
import com.processing.gateway.common.properties.GatewayProperties;
import com.processing.gateway.common.properties.ServiceProperties;
import com.processing.gateway.common.models.Services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Aggregates gateway and downstream health information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final HealthClient healthClient;
    private final ServiceProperties serviceProperties;
    private final HealthProperties healthProperties;
    private final GatewayProperties gatewayProperties;

    /**
     * Checks configured downstream services and builds the gateway health response.
     *
     * @return aggregated health response
     */
    public HealthResponse getDownstreamServicesHealth() {
        String switchUrl = serviceProperties.getSwitchUrl() + healthProperties.getUrl();
        String loggerUrl = serviceProperties.getLoggerUrl() + healthProperties.getUrl();
        String authUrl = serviceProperties.getAuthUrl() + healthProperties.getUrl();
        String cardsUrl = serviceProperties.getCardsUrl() + healthProperties.getUrl();

        Map<String, HealthStatus> downstreamServices = healthClient.sendHealthCheckRequests(
                new HealthRequest(switchUrl, Services.SWITCH.getValue()),
                new HealthRequest(authUrl, Services.AUTH.getValue()),
                new HealthRequest(cardsUrl, Services.CARDS.getValue()),
                new HealthRequest(loggerUrl, Services.LOGGER.getValue()));

        HealthStatus gatewayStatus = downstreamServices.containsValue(HealthStatus.UNAVAILABLE)
                ? HealthStatus.DEGRADED : HealthStatus.OK;

        return new HealthResponse(
                gatewayStatus,
                Services.GATEWAY.getValue(),
                gatewayProperties.getVersion(),
                downstreamServices);
    }
}
