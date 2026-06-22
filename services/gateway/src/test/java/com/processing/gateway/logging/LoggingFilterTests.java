package com.processing.gateway.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.properties.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoggingFilterTests {
    private RequestLoggingFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        var props = new GatewayProperties();
        props.getLogging().setBodies(false);
        props.getLogging().setPretty(false);
        props.getLogging().setExcludedRoutes(Set.of());

        filter = new RequestLoggingFilter(new ObjectMapper(), props);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAddRequestIdHeaderAndContinueChain() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/test");

        // Arg captor is needed to capture wrapped request from chain.doFilter()
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(chain, times(1)).doFilter(requestCaptor.capture(), any(HttpServletResponse.class));

        HttpServletRequest wrappedRequest = requestCaptor.getValue();
        assertNotNull(wrappedRequest);

        String requestId = wrappedRequest.getHeader("X-Request-Id");
        assertNotNull(requestId);
        assertDoesNotThrow(() -> UUID.fromString(requestId));
    }
}
