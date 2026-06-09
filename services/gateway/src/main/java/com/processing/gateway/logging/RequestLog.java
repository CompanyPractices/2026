package com.processing.gateway.logging;

import lombok.Builder;
import lombok.Data;

/**
 * Structured log entry for a processed gateway request
 */
@Data
public class RequestLog {
    private String requestId;
    private String method;
    private String path;
    private Integer responseCode;
    private Long responseTime;

    /**
     * Creates an empty request log entry
     */
    public RequestLog() {
    }

    /**
     * Creates a populated request log entry
     *
     * @param requestId correlation identifier assigned to the request
     * @param method HTTP method
     * @param path request path
     * @param responseCode HTTP response status code
     * @param responseTime request processing time in milliseconds
     */
    @Builder
    public RequestLog(String requestId,
                      String method,
                      String path,
                      Integer responseCode,
                      Long responseTime) {
        this.requestId = requestId;
        this.method = method;
        this.path = path;
        this.responseCode = responseCode;
        this.responseTime = responseTime;
    }
}
