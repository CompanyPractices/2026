package com.processing.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.gateway.validation.TransactionRequestValidator;
import com.processing.gateway.validation.TransactionValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Validates transaction authorization requests before they are proxied to Switch.
 *
 * <p>The filter reads the JSON body once, validates it, and wraps the request
 * with a cached body so Spring Cloud Gateway can still forward the original
 * payload downstream.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class TransactionValidationFilter extends OncePerRequestFilter {

    private static final String TRANSACTIONS_PATH = "/api/transactions";

    private final ObjectMapper objectMapper;
    private final TransactionRequestValidator validator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!isTransactionRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());

        try {
            AuthorizationRequest authorizationRequest = objectMapper.readValue(requestBody, AuthorizationRequest.class);
            validator.validate(authorizationRequest);
        } catch (JsonProcessingException e) {
            writeValidationError(response, "Request body must be valid JSON");
            return;
        } catch (TransactionValidationException e) {
            writeValidationError(response, e.getMessage());
            return;
        }

        filterChain.doFilter(new CachedBodyHttpServletRequest(request, requestBody), response);
    }

    private boolean isTransactionRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && TRANSACTIONS_PATH.equals(request.getRequestURI());
    }

    private void writeValidationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                "VALIDATION_ERROR",
                message,
                Instant.now().toString(),
                null,
                null
        ));
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Async read is not supported");
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());

            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
