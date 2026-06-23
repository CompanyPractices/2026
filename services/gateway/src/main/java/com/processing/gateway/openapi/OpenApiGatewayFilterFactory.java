package com.processing.gateway.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ErrorResponse;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class OpenApiGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {

    private static final String INTERNAL_ROUTE_PREFIX = "/internal";
    private static final String TRANSACTIONS_INTERNAL_ROUTE = "/api/internal/route";
    private static final String TRANSACTIONS_PUBLIC_ROUTE = "/api/transactions";

    private final OpenApiProperties openApiProperties;
    private final ObjectMapper mapper;
    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;

    public OpenApiGatewayFilterFactory(
            OpenApiProperties openApiProperties,
            ObjectMapper mapper,
            ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory) {
        super(NameConfig.class);
        this.openApiProperties = openApiProperties;
        this.mapper = mapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.modifyResponseBodyGatewayFilterFactory = modifyResponseBodyGatewayFilterFactory;
    }

    @Override
    public GatewayFilter apply(NameConfig config) {
        return modifyResponseBodyGatewayFilterFactory.apply(c -> c
                .setRewriteFunction(
                        String.class,
                        String.class,
                        (exchange, body) -> {
                            try {
                                body = modifyOpenApiJson(body);
                            } catch (JsonProcessingException e) {
                                log.error("Error occurred while attempting to read OpenApi json response", e);

                                exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);

                                try {
                                    body = mapper.writeValueAsString(new ErrorResponse(
                                            "Bad Gateway",
                                            "Error reading OpenApi json response",
                                            Instant.now(),
                                            "gateway",
                                            null
                                    ));
                                } catch (JsonProcessingException ex) {
                                    log.error("Error creating error response", e);
                                    body = "{\"message\": \"Error creating error response\"}";
                                }
                            }

                            return Mono.justOrEmpty(body);
                        }));
    }

    private String modifyOpenApiJson(String openApiJson) throws JsonProcessingException {
        OpenAPI openApi = Json.mapper().readValue(openApiJson, OpenAPI.class);
        openApi.setServers(List.of(new Server().url(openApiProperties.getUrl())));

        Paths paths = openApi.getPaths();
        Paths adjustedPaths = new Paths();

        paths.forEach((k, v) -> {
            if (!k.contains(INTERNAL_ROUTE_PREFIX)) {
                adjustedPaths.addPathItem(k, v);
            } else if (k.equals(TRANSACTIONS_INTERNAL_ROUTE)) {
                adjustedPaths.addPathItem(TRANSACTIONS_PUBLIC_ROUTE, v);
            }
        });

        openApi.setPaths(adjustedPaths);

        return Json.mapper().writeValueAsString(openApi);
    }
}
