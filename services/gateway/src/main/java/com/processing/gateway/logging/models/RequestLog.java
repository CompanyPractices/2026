package com.processing.gateway.logging.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.gateway.logging.RequestLoggingFilter;
import lombok.Builder;
import lombok.Data;

/**
 * Structured request log payload written by {@link RequestLoggingFilter}.
 */
@Data
@Builder
public class RequestLog {
    private String requestId;
    private String method;
    private String path;
    private JsonNode requestBody;
    private Integer responseCode;
    private Long responseTime;
    private JsonNode responseBody;
}
