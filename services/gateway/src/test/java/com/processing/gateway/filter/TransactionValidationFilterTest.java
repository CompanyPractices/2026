package com.processing.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.gateway.validation.TransactionRequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionValidationFilterTest {

    private final TransactionValidationFilter filter = new TransactionValidationFilter(
            new ObjectMapper(),
            new TransactionRequestValidator()
    );

    @Test
    void rejectsInvalidTransactionRequestWithValidationError() throws Exception {
        MockHttpServletRequest request = transactionRequest("""
                {
                  "mti": "0100",
                  "stan": "000001",
                  "pan": "400000123456000",
                  "processingCode": "000000",
                  "amount": 150000,
                  "currencyCode": "643",
                  "transmissionDateTime": "2026-06-01T10:30:00Z",
                  "terminalId": "TERM001",
                  "terminalType": "POS",
                  "merchantId": "MERCH12345678901",
                  "mcc": "5411",
                  "acquirerId": "ACQ001"
                }
                """);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> downstreamCalled.set(true));

        assertThat(downstreamCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains(
                "\"error\":\"VALIDATION_ERROR\"",
                "Field 'pan' must be exactly 16 digits"
        );
    }

    @Test
    void passesValidTransactionRequestWithoutLosingBody() throws Exception {
        String body = """
                {
                  "mti": "0100",
                  "stan": "000001",
                  "pan": "4000001234560001",
                  "processingCode": "000000",
                  "amount": 150000,
                  "currencyCode": "643",
                  "transmissionDateTime": "2026-06-01T10:30:00Z",
                  "terminalId": "TERM001",
                  "terminalType": "POS",
                  "merchantId": "MERCH12345678901",
                  "mcc": "5411",
                  "acquirerId": "ACQ001"
                }
                """;
        MockHttpServletRequest request = transactionRequest(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> bodySeenByDownstream = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                bodySeenByDownstream.set(StreamUtils.copyToString(
                        servletRequest.getInputStream(),
                        StandardCharsets.UTF_8
                ))
        );

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(bodySeenByDownstream.get()).isEqualTo(body);
    }

    @Test
    void skipsNonTransactionRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/transactions/search");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> downstreamCalled.set(true));

        assertThat(downstreamCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest transactionRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
