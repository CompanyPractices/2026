package com.processing.gateway.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.processing.gateway.logging.models.RequestLog;
import com.processing.gateway.properties.SmpGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adds request correlation and writes a structured log record for every request.
 *
 * <p>If an incoming {@code X-Request-Id} header is present it is reused;
 * otherwise a new UUID is generated and propagated to the response.</p>
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter {
    private final ObjectMapper mapper;
    private final SmpGatewayProperties properties;

    private static final String ID_HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    public RequestLoggingFilter(ObjectMapper mapper, SmpGatewayProperties properties) {
        this.properties = properties;
        this.mapper = mapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (properties.getLogging().getPretty()) {
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        String incoming = exchange.getRequest().getHeaders().getFirst(ID_HEADER_NAME);
        String requestId = (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();

        var mutatedRequest = exchange.getRequest().mutate()
                .header(ID_HEADER_NAME, requestId)
                .build();
        exchange.getResponse().getHeaders().add(ID_HEADER_NAME, requestId);

        var mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        var requestLogBuilder = RequestLog.builder()
                .requestId(requestId)
                .method(mutatedRequest.getMethod().name())
                .path(mutatedRequest.getPath().value());

        return chain.filter(mutatedExchange).doFinally(signalType -> {
            long responseTime = System.currentTimeMillis() - startTime;

            HttpStatusCode statusCode = mutatedExchange.getResponse().getStatusCode();
            int rawStatusCode = (statusCode != null) ? statusCode.value() : 0;

            var requestLog = requestLogBuilder
                    .responseCode(rawStatusCode)
                    .responseTime(responseTime)
                    .build();

            MDC.put(MDC_KEY, requestId);
            try {
                String mappedLog = mapper.writeValueAsString(requestLog);
                log.info(mappedLog);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred while mapping log message", e);
            } finally {
                MDC.remove(MDC_KEY);
            }
        });
    }
}
