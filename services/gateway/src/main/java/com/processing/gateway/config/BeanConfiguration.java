package com.processing.gateway.config;

import com.processing.gateway.health.HealthProperties;
import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Declares gateway infrastructure beans shared by filters and services.
 */
@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {
    private final HealthProperties healthProperties;

    /**
     * Creates the JDK HTTP client used for downstream health checks.
     *
     * @return HTTP client with configured connect timeout
     */
    @Bean
    public HttpClient httpClient(Executor executor) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(healthProperties.getConnectionTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(executor)
                .build();
    }

    @Bean
    public Executor executorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("health-check-");
        executor.initialize();

        return executor;
    }

    /**
     * Creates the cache manager used by response caching.
     *
     * @return caffeine-backed cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager("gateway-cache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }

    /**
     * Exposes the gateway cache as a direct bean for filters.
     *
     * @param cacheManager configured cache manager
     * @return cache named {@code gateway-cache}
     */
    @Bean
    public Cache gatewayCache(CacheManager cacheManager) {
        return cacheManager.getCache("gateway-cache");
    }
}
