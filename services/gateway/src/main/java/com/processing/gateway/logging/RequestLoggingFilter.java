package com.processing.gateway.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.processing.gateway.logging.models.RequestLog;
import com.processing.gateway.properties.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds request correlation and writes a structured log record for every request.
 *
 * <p>If an incoming {@code X-Request-Id} header is present it is reused;
 * otherwise a new UUID is generated and propagated to the response.</p>
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;
    private final GatewayProperties properties;

    private static final String ID_HEADER_NAME = "X-Request-Id";

    private static final Pattern PAN_PATTERN = Pattern.compile("(?i)\"pan\"\\s*:\\s*\"(\\d{6})\\d{6,9}(\\d{4})\"");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)\"cvv\"\\s*:\\s*\"\\d{3,4}\"");

    private static final String MASKED_PAN = "\"pan\":\"$1****$2\"";
    private static final String MASKED_CVV = "\"cvv\":\"***\"";

    public RequestLoggingFilter(ObjectMapper mapper, GatewayProperties properties) {
        this.properties = properties;
        this.mapper = mapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (properties.getLogging().getPretty()) {
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (properties.getLogging().getExcludedRoutes().contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        String incoming = request.getHeader(ID_HEADER_NAME);
        String requestId = incoming != null ? incoming : UUID.randomUUID().toString();

        var wrappedRequest = new MutableHeadersRequestWrapper(request);
        wrappedRequest.setHeader(ID_HEADER_NAME, requestId);

        response.setHeader(ID_HEADER_NAME, requestId);

        var contentCachedRequest = new ContentCachingRequestWrapper(wrappedRequest);
        var contentCachedResponse = new ContentCachingResponseWrapper(response);

        MDC.put("requestId", requestId);

        try {
            filterChain.doFilter(contentCachedRequest, contentCachedResponse);
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;

            var requestLogBuilder = RequestLog.builder()
                    .requestId(requestId)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .responseCode(response.getStatus())
                    .responseTime(responseTime);

            if (properties.getLogging().getBodies()) {
                logBodies(contentCachedRequest, contentCachedResponse, requestLogBuilder);
            }

            contentCachedResponse.copyBodyToResponse();

            try {
                String mappedLog = mapper.writeValueAsString(requestLogBuilder.build());
                log.info(mappedLog);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred while mapping log message", e);
            } finally {
                MDC.remove("requestId");
            }

        }
    }

    private void logBodies(ContentCachingRequestWrapper request,
                           ContentCachingResponseWrapper response,
                           RequestLog.RequestLogBuilder logBuilder) {
        String reqBodyStr = request.getContentAsString();
        String resBodyStr = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        String reqContentType = request.getContentType();
        String resContentType = response.getContentType();

        if (reqContentType != null
                && request.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)
                && !reqBodyStr.isEmpty()) {
            reqBodyStr = maskData(PAN_PATTERN, MASKED_PAN, reqBodyStr);
            reqBodyStr = maskData(CVV_PATTERN, MASKED_CVV, reqBodyStr);

            logBuilder.requestBody(reqBodyStr);
        }

        if (resContentType != null
                && response.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)
                && !resBodyStr.isEmpty()) {
            resBodyStr = maskData(PAN_PATTERN, MASKED_PAN, resBodyStr);
            resBodyStr = maskData(CVV_PATTERN, MASKED_CVV, resBodyStr);

            logBuilder.responseBody(resBodyStr);
        }
    }

    private String maskData(Pattern pattern, String mask, String data) {
        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            return matcher.replaceAll(mask);
        }

        return data;
    }
}
