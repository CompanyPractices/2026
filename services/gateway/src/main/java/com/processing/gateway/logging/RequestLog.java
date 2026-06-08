package com.processing.gateway.logging;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestLog {
    private String requestId;
    private String method;
    private String path;
    private Integer responseCode;
    private Long responseTime;
}
