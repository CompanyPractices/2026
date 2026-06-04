package com.processing.gateway.logger;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RequestLog {
    private UUID requestId;
    private String method;
    private String path;
    private Integer responseCode;
    private Long responseTime;
}
