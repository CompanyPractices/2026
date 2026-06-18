package com.processing.gateway.health;

import com.processing.gateway.health.models.HealthResponse;
import com.processing.gateway.health.models.HealthStatus;
import com.processing.gateway.properties.GatewayProperties;
import com.processing.gateway.properties.ServiceProperties;
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
    private static final String GATEWAY_SERVICE_NAME = "gateway";
    private static final String SWITCH_SERVICE_NAME = "switch";
    private static final String AUTH_SERVICE_NAME = "authorization";
    private static final String CARD_MGMT_SERVICE_NAME = "cardManagement";
    private static final String LOGGER_SERVICE_NAME = "logger";

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

        Map<String, HealthStatus> downstreamServices = Map.of(
                SWITCH_SERVICE_NAME, healthClient.sendHealthCheckRequest(switchUrl, SWITCH_SERVICE_NAME),
                AUTH_SERVICE_NAME, healthClient.sendHealthCheckRequest(authUrl, AUTH_SERVICE_NAME),
                CARD_MGMT_SERVICE_NAME, healthClient.sendHealthCheckRequest(cardsUrl, CARD_MGMT_SERVICE_NAME),
                LOGGER_SERVICE_NAME, healthClient.sendHealthCheckRequest(loggerUrl, LOGGER_SERVICE_NAME));

        HealthStatus gatewayStatus = downstreamServices.containsValue(HealthStatus.UNAVAILABLE)
                ? HealthStatus.DEGRADED : HealthStatus.OK;

        return new HealthResponse(
                gatewayStatus,
                GATEWAY_SERVICE_NAME,
                gatewayProperties.getVersion(),
                downstreamServices);
    }
}
