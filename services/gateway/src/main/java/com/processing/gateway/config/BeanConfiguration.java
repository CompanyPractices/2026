package com.processing.gateway.config;

import com.processing.gateway.properties.HealthProperties;
import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Defines shared infrastructure beans used by the gateway.
 */
@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {
    private final HealthProperties healthProperties;

    /**
     * Creates an HTTP client for gateway-to-service calls
     *
     * @return configured HTTP client with connection timeout and redirect support
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(healthProperties.getConnectionTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager("gateway-cache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }

    @Bean
    public Cache gatewayCache(CacheManager cacheManager) {
        return cacheManager.getCache("gateway-cache");
    }
}
