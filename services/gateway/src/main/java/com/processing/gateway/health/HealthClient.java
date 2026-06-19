package com.processing.gateway.health;

import com.processing.gateway.common.models.Headers;
import com.processing.gateway.health.models.HealthRequest;
import com.processing.gateway.health.models.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
     * @param requests containing services names and URL's
     * @return {@link HealthStatus#OK} for HTTP 200, otherwise
     *         {@link HealthStatus#UNAVAILABLE}
     */
    public Map<String, HealthStatus> sendHealthCheckRequests(HealthRequest... requests) {
        List<CompletableFuture<ServiceStatus>> futures = new ArrayList<>();
        for (HealthRequest request : requests) {
            futures.add(sendRequestAsync(request.uri(), request.service()));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(ServiceStatus::service, ServiceStatus::status));
    }

    private CompletableFuture<ServiceStatus> sendRequestAsync(String uri, String service) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .header(Headers.X_REQUEST_ID.getValue(), UUID.randomUUID().toString())
                .timeout(Duration.ofSeconds(healthProperties.getRequestTimeout()))
                .build();

        CompletableFuture<HttpResponse<String>> future = httpClient
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());

        return future
            .thenApply(response -> {
                if (response.statusCode() == HttpStatus.OK.value()) {
                    return new ServiceStatus(service, HealthStatus.OK);
                }

                return new ServiceStatus(service, HealthStatus.UNAVAILABLE);
            })
            .exceptionally(e -> {
                log.error("Exception occurred while checking health of service {}", service, e);
                return new ServiceStatus(service, HealthStatus.UNAVAILABLE);
            });
    }

    private record ServiceStatus(String service, HealthStatus status) {}
}
