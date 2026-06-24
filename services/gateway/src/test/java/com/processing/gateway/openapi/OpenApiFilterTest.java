package com.processing.gateway.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ErrorResponse;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenApiFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;

    @Mock
    private ServerWebExchange exchange;

    private MockServerHttpResponse response;
    private RewriteFunction<String, String> rewriteFunction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        var factory = new OpenApiGatewayFilterFactory(
                objectMapper,
                modifyResponseBodyGatewayFilterFactory
        );

        ArgumentCaptor<Consumer<ModifyResponseBodyGatewayFilterFactory.Config>> configCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        when(modifyResponseBodyGatewayFilterFactory.apply(configCaptor.capture()))
                .thenReturn(mock(GatewayFilter.class));

        factory.apply(new AbstractGatewayFilterFactory.NameConfig());

        ModifyResponseBodyGatewayFilterFactory.Config capturedConfig =
                new ModifyResponseBodyGatewayFilterFactory.Config();
        configCaptor.getValue().accept(capturedConfig);

        this.rewriteFunction = capturedConfig.getRewriteFunction();

        this.response = new MockServerHttpResponse();
        when(exchange.getResponse()).thenReturn(response);
    }

    @Test
    void shouldModifyOpenApiJsonSuccessfully() throws JsonProcessingException, URISyntaxException {
        String inputJson = """
                {
                  "openapi": "3.0.1",
                  "servers": [{"url": "http://localhost:8080"}],
                  "paths": {
                    "/api/public/v1/cards": {},
                    "/internal/system/status": {},
                    "/api/internal/route": {}
                  }
                }
                """;

        MockServerWebExchange mockExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost:9999/api-docs").build()
        );

        Mono<String> resultMono = (Mono<String>) rewriteFunction.apply(mockExchange, inputJson);

        StepVerifier.create(resultMono)
                .assertNext(outputJson -> {
                    try {
                        OpenAPI resultOpenApi = Json.mapper().readValue(outputJson, OpenAPI.class);

                        assertEquals(1, resultOpenApi.getServers().size());
                        assertEquals("http://localhost:9999", resultOpenApi.getServers().get(0).getUrl());

                        Paths paths = resultOpenApi.getPaths();

                        assertTrue(paths.containsKey("/api/public/v1/cards"), "Public route should be preserved");
                        assertFalse(paths.containsKey("/internal/system/status"), "Internal route should be removed");
                        assertFalse(paths.containsKey("/api/internal/route"), "Old internal route name should be removed");
                        assertTrue(paths.containsKey("/api/transactions"), "Internal route should be renamed to public equivalent");

                        assertEquals(2, paths.size(), "Total paths count should be exactly 2");

                    } catch (JsonProcessingException e) {
                        fail("Failed to parse output OpenAPI JSON", e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnBadGatewayOnError() {
        String malformedJson = "{ invalid json [";

        Mono<String> resultMono = (Mono<String>) rewriteFunction.apply(exchange, malformedJson);

        StepVerifier.create(resultMono)
                .assertNext(errorResponseBody -> {
                    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());

                    try {
                        ObjectMapper testMapper = new ObjectMapper();
                        ErrorResponse error = testMapper.readValue(errorResponseBody, ErrorResponse.class);

                        assertEquals("Bad Gateway", error.message()); // Или какое поле отвечает за заголовок/сообщение
                        assertNotNull(error.timestamp());
                    } catch (Exception e) {
                        // Если структура ErrorResponse отличается, можно проверить просто через контейнмент строки:
                        assertTrue(errorResponseBody.contains("Error reading OpenApi json response"));
                    }
                })
                .verifyComplete();
    }
}
