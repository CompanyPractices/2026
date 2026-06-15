package com.processing.gateway.client;

import com.processing.gateway.enums.HealthStatus;
import com.processing.gateway.properties.HealthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client wrapper used by gateway health checks.
 *
 * <p>It converts downstream HTTP and network failures into {@link HealthStatus}
 * values so health aggregation does not leak transport exceptions.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthClient {
    private final HttpClient httpClient;
    private final HealthProperties healthProperties;

    /**
     * Sends a health-check request to a downstream service.
     *
     * @param url full health-check URL
     * @param serviceName service name used in logs
     * @return {@link HealthStatus#OK} for HTTP 200, otherwise
     *         {@link HealthStatus#UNAVAILABLE}
     */
    public HealthStatus sendHealthCheckRequest(String url, String serviceName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(healthProperties.getRequestTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return HealthStatus.OK;
            }

            return HealthStatus.UNAVAILABLE;
        } catch (Exception e) {
            log.error("Exception occurred while checking health of service {}", serviceName, e);
            return HealthStatus.UNAVAILABLE;
        }
    }
}
