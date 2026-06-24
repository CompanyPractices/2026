package com.processing.gateway.validation;

import com.processing.gateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionValidationFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final TransactionValidationFilter filter = new TransactionValidationFilter(
            Jackson2ObjectMapperBuilder.json().build(),
            new TransactionRequestValidator(),
            new GatewayMetrics(meterRegistry)
    );

    @Test
    void rejectsInvalidTransactionRequestWithValidationError() {
        MockServerWebExchange exchange = transactionExchange("""
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
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.filter(exchange, calledChain(downstreamCalled)).block();

        assertThat(downstreamCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(
                "\"error\":\"VALIDATION_ERROR\"",
                "Field 'pan' must be exactly 16 digits",
                "\"serviceName\":\"gateway\""
        );
        assertThat(meterRegistry.counter(
                "gateway.requests.rejected",
                "reason", "validation_invalid_request",
                "service", "gateway"
        ).count()).isEqualTo(1);
    }

    @Test
    void passesValidTransactionRequestWithoutLosingBody() {
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
        MockServerWebExchange exchange = transactionExchange(body);
        AtomicReference<String> bodySeenByDownstream = new AtomicReference<>();

        filter.filter(exchange, downstreamBodyChain(bodySeenByDownstream)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodySeenByDownstream.get()).isEqualTo(body);
    }

    @Test
    void skipsNonTransactionRequests() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/transactions/search"));
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);

        filter.filter(exchange, calledChain(downstreamCalled)).block();

        assertThat(downstreamCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private MockServerWebExchange transactionExchange(String body) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.POST, "/api/transactions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body));
    }

    private GatewayFilterChain calledChain(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }

    private GatewayFilterChain downstreamBodyChain(AtomicReference<String> bodySeenByDownstream) {
        return exchange -> DataBufferUtils.join(exchange.getRequest().getBody())
                .doOnNext(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    bodySeenByDownstream.set(new String(bytes, StandardCharsets.UTF_8));
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                })
                .then();
    }
}
