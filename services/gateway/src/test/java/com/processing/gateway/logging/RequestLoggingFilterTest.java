package com.processing.gateway.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.properties.SmpGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @Mock
    private GatewayFilterChain chain;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        SmpGatewayProperties properties = mock(SmpGatewayProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getLogging().getPretty()).thenReturn(false);
        when(properties.getLogging().getBodies()).thenReturn(false);
        when(properties.getLogging().getExcludedRoutes()).thenReturn(Set.of());

        ObjectMapper objectMapper = new ObjectMapper();
        filter = new RequestLoggingFilter(objectMapper, properties, new LogDataMasker());

        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void shouldReuseExistingRequestId() {
        // Arrange
        String existingId = "test-request-id-12345";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards")
                        .header("X-Request-Id", existingId)
                        .build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        ServerWebExchange mutatedExchange = exchangeCaptor.getValue();
        assertEquals(existingId, mutatedExchange.getRequest().getHeaders().getFirst("X-Request-Id"));

        assertEquals(existingId, exchange.getResponse().getHeaders().getFirst("X-Request-Id"));

        assertFalse(listAppender.list.isEmpty(), "Log message should be generated");
        ILoggingEvent logEvent = listAppender.list.getFirst();

        assertEquals(existingId, logEvent.getMDCPropertyMap().get("requestId"));

        String logMessage = logEvent.getFormattedMessage();
        assertTrue(logMessage.contains("\"requestId\":\"" + existingId + "\""));
        assertTrue(logMessage.contains("\"method\":\"GET\""));
        assertTrue(logMessage.contains("\"path\":\"/api/cards\""));
        assertTrue(logMessage.contains("\"responseCode\":200"));
    }

    @Test
    void shouldGenerateUuidActHeaderIsMissing() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cards").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        ServerWebExchange mutatedExchange = exchangeCaptor.getValue();
        String generatedId = mutatedExchange.getRequest().getHeaders().getFirst("X-Request-Id");

        assertNotNull(generatedId);
        assertDoesNotThrow(() -> UUID.fromString(generatedId), "Generated ID must be a valid UUID");

        assertEquals(generatedId, exchange.getResponse().getHeaders().getFirst("X-Request-Id"));

        ILoggingEvent logEvent = listAppender.list.getFirst();
        String logMessage = logEvent.getFormattedMessage();

        assertEquals(generatedId, logEvent.getMDCPropertyMap().get("requestId"));
        assertTrue(logMessage.contains("\"requestId\":\"" + generatedId + "\""));
        assertTrue(logMessage.contains("\"method\":\"POST\""));
        assertTrue(logMessage.contains("\"responseCode\":201"));
    }

    @Test
    @DisplayName("Should handle missing status code safely (log 0)")
    void shouldHandleMissingStatusCodeSafely() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/unknown").build()
        );

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        ILoggingEvent logEvent = listAppender.list.getFirst();
        String logMessage = logEvent.getFormattedMessage();

        assertTrue(logMessage.contains("\"responseCode\":0"), "Should log 0 if status code is null");
    }
}
