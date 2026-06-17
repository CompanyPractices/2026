package com.processing.gateway.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.processing.gateway.logging.RequestLog;
import com.processing.gateway.wrapper.MutableHeadersRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
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
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    private static final String ID_HEADER_NAME = "X-Request-Id";

    private static final Pattern PAN_PATTERN = Pattern.compile("(?i)\"pan\"\\s*:\\s*\"(\\d{6})\\d{6,9}(\\d{4})\"");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)\"cvv\"\\s*:\\s*\"\\d{3,4}\"");

    private static final String MASKED_PAN = "\"pan\":\"$1****$2\"";
    private static final String MASKED_CVV = "\"cvv\":\"***\"";

    private static final List<String> BODY_LOG_EXCLUDED_ROUTES = List.of(
            "/actuator/prometheus"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
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
            var mapper = objectMapper.copy();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            long responseTime = System.currentTimeMillis() - startTime;

            var requestLogBuilder = RequestLog.builder()
                    .requestId(requestId)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .responseCode(response.getStatus())
                    .responseTime(responseTime);

            if (!BODY_LOG_EXCLUDED_ROUTES.contains(request.getRequestURI())) {
                var resBodyStr = new String(contentCachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                var reqBodyStr = contentCachedRequest.getContentAsString();

                try {
                    if (!resBodyStr.isEmpty()
                            && contentCachedResponse.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)) {
                        resBodyStr = maskData(PAN_PATTERN, MASKED_PAN, resBodyStr);
                        resBodyStr = maskData(CVV_PATTERN, MASKED_CVV, resBodyStr);

                        var resBody = mapper.readValue(resBodyStr, JsonNode.class);
                        requestLogBuilder.responseBody(resBody);
                    }
                } catch (JsonProcessingException e) {
                    log.error("Error parsing response body for logging", e);
                }

                try {
                    if (!reqBodyStr.isEmpty()
                            && contentCachedRequest.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)) {
                        reqBodyStr = maskData(PAN_PATTERN, MASKED_PAN, reqBodyStr);
                        reqBodyStr = maskData(CVV_PATTERN, MASKED_CVV, reqBodyStr);

                        var reqBody = mapper.readValue(reqBodyStr, JsonNode.class);
                        requestLogBuilder.requestBody(reqBody);
                    }
                } catch (JsonProcessingException e) {
                    log.error("Error parsing request body for logging", e);
                }
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

    private String maskData(Pattern pattern, String mask, String data) {
        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            return matcher.replaceAll(mask);
        }

        return data;
    }
}
