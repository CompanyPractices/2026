package com.processing.gateway.caching;

import com.processing.gateway.metrics.GatewayMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseCachingFilterTest {

    @Mock
    private Cache cache;

    @Mock
    private GatewayMetrics gatewayMetrics;

    @Mock
    private GatewayFilterChain chain;

    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        var factory = new ResponseCachingGatewayFilterFactory(cache, gatewayMetrics);
        this.filter = factory.apply(new AbstractGatewayFilterFactory.NameConfig());
    }

    @Test
    void shouldClearCacheOnNonGetSuccess() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cards").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(cache).clear();
        verify(gatewayMetrics).recordCardsCacheInvalidation();
        verifyNoMoreInteractions(cache, gatewayMetrics);
    }

    @Test
    void shouldNotClearCacheOnNonGetFailure() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cards").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(cache, never()).clear();
        verify(gatewayMetrics, never()).recordCardsCacheInvalidation();
    }

    @Test
    void shouldReturnCachedResponseOnCacheHit() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards").queryParam("page", "1").build()
        );
        String expectedCacheKey = "/api/cards{page=[1]}";
        byte[] cachedBody = "{\"status\":\"cached\"}".getBytes(StandardCharsets.UTF_8);

        when(cache.get(expectedCacheKey, byte[].class)).thenReturn(cachedBody);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(gatewayMetrics).recordCardsCacheHit();
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        assertEquals("HIT", exchange.getResponse().getHeaders().getFirst("X-Cache"));

        verifyNoInteractions(chain);
    }

    @Test
    void shouldDecorateResponseAndSaveToCacheOnMiss() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards").build()
        );
        String expectedCacheKey = "/api/cards{}";

        when(cache.get(expectedCacheKey, byte[].class)).thenReturn(null);

        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange serverWebExchange = invocation.getArgument(0);

            var response = serverWebExchange.getResponse();
            var bufferFactory = response.bufferFactory();
            var dataBuffer = bufferFactory.wrap("data".getBytes());

            assertEquals("MISS", response.getHeaders().getFirst("X-Cache"));

            return response.writeWith(Flux.just(dataBuffer));
        });

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(gatewayMetrics).recordCardsCacheMiss();
        verify(cache).put(eq(expectedCacheKey), any(byte[].class));

        StepVerifier.create(exchange.getResponse().getBody())
                .consumeNextWith(dataBuffer -> {
                    var bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    assertEquals("data", new String(bytes));
                })
                .verifyComplete();
    }
}
