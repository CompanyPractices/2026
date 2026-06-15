package com.processing.gateway.logging;

import lombok.Builder;
import lombok.Data;

/**
 * Structured request log payload written by {@link com.processing.gateway.filter.RequestLoggingFilter}.
 */
@Data
@Builder
public class RequestLog {
    private String requestId;
    private String method;
    private String path;
    private Integer responseCode;
    private Long responseTime;
}
