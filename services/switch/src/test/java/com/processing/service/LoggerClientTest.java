package com.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.enums.TransactionStatus;
import com.processing.model.LogResponse;
import com.processing.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LoggerClientTest {

    private MockRestServiceServer mockServer;
    private LoggerClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new LoggerClient(SwitchTestData.defaultProperties(), builder.build());
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void log_whenLoggerReturnsSuccess_returnsTrue() throws Exception {
        UUID id = UUID.randomUUID();
        LogResponse logResponse = new LogResponse(id.toString(), "stored");
        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(logResponse), MediaType.APPLICATION_JSON));

        assertThat(client.log(sampleTransaction(id))).isTrue();
    }

    @Test
    void log_whenLoggerFailsAllRetries_returnsFalse() {
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        assertThat(client.log(sampleTransaction(UUID.randomUUID()))).isFalse();
    }

    @Test
    void log_whenSecondAttemptSucceeds_returnsTrue() throws Exception {
        UUID id = UUID.randomUUID();
        LogResponse logResponse = new LogResponse(id.toString(), "stored");

        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(logResponse), MediaType.APPLICATION_JSON));

        assertThat(client.log(sampleTransaction(id))).isTrue();
    }

    private static Transaction sampleTransaction(UUID id) {
        return new Transaction(
                id,
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                150_000L,
                "643",
                "TERM001",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                "ISS001",
                null,
                TransactionStatus.APPROVED,
                null,
                "TEST01",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }
}
