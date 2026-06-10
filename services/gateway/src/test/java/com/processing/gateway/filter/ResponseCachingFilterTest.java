package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ServiceUnavailableResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseCachingFilterTest {

    @Mock
    private Cache cache;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private ResponseCachingFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new ResponseCachingFilter(cache, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should skip caching when URI does not contain prefix")
    void shouldSkipCachingWhenUriDoesNotMatch() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/users/123");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(cache);
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

        request.setRequestURI(uri);
        request.setQueryString(queryString);
        request.setMethod(HttpMethod.GET.name());

        when(cache.get(cacheKey, String.class)).thenReturn(cachedJson);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        assertEquals("HIT", response.getHeader("X-Cache"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(cachedJson.length(), response.getContentLength());
        assertEquals(cachedJson, response.getContentAsString());

        verifyNoInteractions(filterChain);
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

        doAnswer(invocation -> {
            HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(200);
            res.getWriter().write(responseBody);
            return null;
        }).when(filterChain).doFilter(eq(request), any());

        when(cache.get(cacheKey, String.class)).thenReturn(null);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        assertEquals("MISS", response.getHeader("X-Cache"));
        assertEquals(responseBody, response.getContentAsString());

        verify(cache, times(1)).put(cacheKey, responseBody);
    }

    @Test
    @DisplayName("Should not cache response if HTTP status is not 200")
    void shouldNotCacheWhenStatusIsNot200() throws ServletException, IOException {
        // Arrange
        String uri = "/api/cards/500";
        String responseBody = "{\"id\": 123, \"status\": \"not-found\"}";

        request.setRequestURI(uri);

        doAnswer(invocation -> {
            HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(404);
            res.getWriter().write(responseBody);
            return null;
        }).when(filterChain).doFilter(eq(request), any());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(404, response.getStatus());
        assertEquals(responseBody, response.getContentAsString());
        assertNull(response.getHeader("X-Cache"));
        verify(cache, never()).put(any(), any());
    }

    @Test
    @DisplayName("Should return HTTP 502 Bad Gateway when exception occurs")
    void shouldReturn502OnException() throws ServletException, IOException {
        // Arrange
        String responseBody = objectMapper.writeValueAsString(
                new ServiceUnavailableResponse(
                "SERVICE_UNAVAILABLE",
                "Card Management Service is temporarily unavailable",
                "Card Management Service"
        ));

        request.setRequestURI("/api/cards/error");
        request.setMethod(HttpMethod.GET.name());

        doThrow(new RuntimeException("Downstream timeout"))
                .when(filterChain).doFilter(eq(request), any());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(503, response.getStatus());
        assertEquals(responseBody, response.getContentAsString());
        verify(cache, never()).put(any(), any());
    }

    @Test
    @DisplayName("Should skip caching when request method is not GET")
    void shouldSkipCachingWhenMethodIsNotGet() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/cards/update");
        request.setMethod(HttpMethod.POST.name());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(cache);
        assertNull(response.getHeader("X-Cache"));
    }
}
