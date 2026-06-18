package com.processing.merchantacquirer.exceptions;

import com.processing.merchantacquirer.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalServiceExceptionTest {
    @Test
    void parseErrorBody() {
        String time = Instant.now().toString();
        String body = "{\"error\":\"External\",\"message\":\"down\",\"timestamp\":\"" + time + "\","
                + "\"serviceName\":\"Card management\",\"retryAfterMs\":\"1500\"}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                HttpHeaders.EMPTY, body.getBytes(), StandardCharsets.UTF_8);
        ExternalServiceException result = ExternalServiceException.fromResponse(ex);

        assertEquals("Card management", result.getServiceName());
        assertEquals("down", result.getMessage());
        assertEquals("1500", result.getRetryAfterMs());
    }

    @Test
    void emptyErrorResponse() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_GATEWAY, "Bad gateway",
                HttpHeaders.EMPTY, "<<json>>".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        ExternalServiceException result = ExternalServiceException.fromResponse(ex);

        assertEquals("Merchant acquirer simulator", result.getServiceName());
        assertEquals("0", result.getRetryAfterMs());
    }
}
