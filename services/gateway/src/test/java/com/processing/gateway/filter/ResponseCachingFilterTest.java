package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNullApi;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseCachingFilterTest {

    private ObjectMapper objectMapper;
    private TestCache cache;
    private ResponseCachingFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cache = new TestCache();
        filter = new ResponseCachingFilter(cache, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should skip caching when URI does not contain prefix")
    void shouldSkipCachingWhenUriDoesNotMatch() throws ServletException, IOException {
        // Arrange
        CountingFilterChain filterChain = new CountingFilterChain();
        request.setRequestURI("/api/users/123");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(1, filterChain.callCount);
        assertEquals(0, cache.getCount);
        assertEquals(0, cache.putCount);
        assertNull(response.getHeader("X-Cache"));
    }

    @Test
    @DisplayName("Should return CACHE_HIT and response body when data is in cache")
    void shouldReturnCacheHitWhenDataPresentInCache() throws ServletException, IOException {
        // Arrange
        String uri = "/api/cards/list";
        String queryString = "?userId=1";
        String cacheKey = uri + queryString;
        String cachedJson = "{\"cards\": []}";
        CountingFilterChain filterChain = new CountingFilterChain();

        request.setRequestURI(uri);
        request.setQueryString(queryString);
        request.setMethod(HttpMethod.GET.name());
        cache.put(cacheKey, cachedJson);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        assertEquals("HIT", response.getHeader("X-Cache"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(cachedJson.length(), response.getContentLength());
        assertEquals(cachedJson, response.getContentAsString());
        assertEquals(0, filterChain.callCount);
    }

    @Test
    @DisplayName("Should return CACHE_MISS and save result to cache on HTTP 200")
    void shouldReturnCacheMissAndSaveToCacheOnSuccess() throws ServletException, IOException {
        // Arrange
        String uri = "/api/cards/123";
        String cacheKey = uri + "null";
        String responseBody = "{\"id\": 123, \"status\": \"active\"}";

        request.setRequestURI(uri);
        request.setMethod(HttpMethod.GET.name());

        //Act
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse res = (HttpServletResponse) servletResponse;
            res.setStatus(200);
            res.getWriter().write(responseBody);
        });

        // Assert
        assertEquals(200, response.getStatus());
        assertEquals("MISS", response.getHeader("X-Cache"));
        assertEquals(responseBody, response.getContentAsString());
        assertEquals(responseBody, cache.get(cacheKey, String.class));
    }

    @Test
    @DisplayName("Should not cache response if HTTP status is not 200")
    void shouldNotCacheWhenStatusIsNot200() throws ServletException, IOException {
        // Arrange
        String uri = "/api/cards/500";
        String responseBody = "{\"id\": 123, \"status\": \"not-found\"}";

        request.setRequestURI(uri);

        // Act
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse res = (HttpServletResponse) servletResponse;
            res.setStatus(404);
            res.getWriter().write(responseBody);
        });

        // Assert
        assertEquals(404, response.getStatus());
        assertEquals(responseBody, response.getContentAsString());
        assertNull(response.getHeader("X-Cache"));
        assertEquals(0, cache.putCount);
    }

    @Test
    @DisplayName("Should return HTTP 503 when downstream connectivity exception occurs")
    void shouldReturn503OnDownstreamException() throws ServletException, IOException {
        String responseBody = objectMapper.writeValueAsString(
                new ServiceUnavailableResponse(
                        "SERVICE_UNAVAILABLE",
                        "Card Management Service is temporarily unavailable",
                        "cardManagement"
                ));

        request.setRequestURI("/api/cards/error");
        request.setMethod(HttpMethod.GET.name());

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new ResourceAccessException("Downstream timeout");
        });

        assertEquals(503, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(responseBody, response.getContentAsString());
        assertEquals(0, cache.putCount);
    }

    @Test
    @DisplayName("Should rethrow non-downstream exception")
    void shouldRethrowNonDownstreamException() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        request.setRequestURI("/api/cards/error");
        request.setMethod(HttpMethod.GET.name());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                    throw exception;
                }));

        assertEquals(exception, thrown);
        assertEquals(0, cache.putCount);
    }

    @Test
    @DisplayName("Should skip caching when request method is not GET")
    void shouldSkipCachingWhenMethodIsNotGet() throws ServletException, IOException {
        // Arrange
        CountingFilterChain filterChain = new CountingFilterChain();
        request.setRequestURI("/api/cards/update");
        request.setMethod(HttpMethod.POST.name());

        // Act
        filter.doFilter(request, response, filterChain);

        assertEquals(1, filterChain.callCount);
        assertEquals(0, cache.getCount);
        assertEquals(0, cache.putCount);
        assertNull(response.getHeader("X-Cache"));
    }

    @Test
    @DisplayName("Should clear cache after successful card mutation")
    void shouldClearCacheAfterSuccessfulCardMutation() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/cards/123");
        request.setMethod(HttpMethod.PATCH.name());

        // Act
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse res = (HttpServletResponse) servletResponse;
            res.setStatus(200);
        });

        // Assert
        assertEquals(1, cache.clearCount);
    }

    @Test
    @DisplayName("Should keep cache after failed card mutation")
    void shouldKeepCacheAfterFailedCardMutation() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/cards/123");
        request.setMethod(HttpMethod.DELETE.name());

        // Act
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse res = (HttpServletResponse) servletResponse;
            res.setStatus(404);
        });

        // Assert
        assertEquals(0, cache.clearCount);
    }

    private static final class CountingFilterChain implements FilterChain {
        private int callCount;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) {
            callCount++;
        }
    }

    private static final class TestCache implements Cache {
        private final Map<Object, Object> values = new HashMap<>();
        private int getCount;
        private int putCount;
        private int clearCount;

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public Object getNativeCache() {
            return values;
        }

        @Override
        public ValueWrapper get(Object key) {
            Object value = values.get(key);
            return value == null ? null : new SimpleValueWrapper(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            getCount++;
            Object value = values.get(key);
            return value == null ? null : (T) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            return (T) values.computeIfAbsent(key, ignored -> {
                try {
                    return valueLoader.call();
                } catch (Exception e) {
                    throw new ValueRetrievalException(key, valueLoader, e);
                }
            });
        }

        @Override
        public void put(Object key, Object value) {
            putCount++;
            values.put(key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Object existing = values.putIfAbsent(key, value);
            return existing == null ? null : new SimpleValueWrapper(existing);
        }

        @Override
        public void evict(Object key) {
            values.remove(key);
        }

        @Override
        public void clear() {
            clearCount++;
            values.clear();
        }
    }
}
