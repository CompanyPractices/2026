package com.processing.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.config.RetryFactory;
import com.processing.config.SwitchProperties;
import com.processing.common.dto.transaction.LogResponse;
import com.processing.common.dto.transaction.Transaction;
import com.processing.common.dto.transaction.TransactionStatus;
import com.processing.exception.LoggerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


import java.time.Instant;
import java.util.List;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        client = new LoggerClient(
                SwitchTestData.defaultProperties(),
                builder.build(),
                RetryFactory.loggerRetry(SwitchTestData.defaultProperties()));
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }


    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }


    @Test
    void log_whenLoggerReturnsSuccess_returnsTrue() throws Exception {
        UUID id = UUID.randomUUID();
        LogResponse logResponse = new LogResponse(id, "stored");
        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(logResponse), MediaType.APPLICATION_JSON));


        assertThat(client.log(sampleTransaction(id))).isTrue();
    }


    @Test
    void log_whenLoggerFailsAllRetries_throwsLoggerException() {
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }


        assertThrows(LoggerException.class, () ->
                client.log(sampleTransaction(UUID.randomUUID())));
    }


    @Test
    void log_whenSecondAttemptSucceeds_returnsTrue() throws Exception {
        UUID id = UUID.randomUUID();
        LogResponse logResponse = new LogResponse(id, "stored");


        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo("http://localhost:8088/api/internal/log"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(logResponse), MediaType.APPLICATION_JSON));


        assertThat(client.log(sampleTransaction(id))).isTrue();
    }


    @Test
    void log_whenMaxAttemptsExceedsBackoffList_usesLastBackoffValue() {
        SwitchProperties properties = new SwitchProperties(
                "1.0.0",
                SwitchTestData.BIN_ROUTING,
                "http://localhost:8083",
                "http://localhost:8088",
                SwitchTestData.defaultHttp(),
                new SwitchProperties.RetryProperties(5, List.of(0L, 0L, 0L))
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LoggerClient extendedClient = new LoggerClient(
                properties, builder.build(), RetryFactory.loggerRetry(properties));


        for (int i = 0; i < 5; i++) {
            server.expect(requestTo("http://localhost:8088/api/internal/log"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }


        assertThrows(LoggerException.class, () ->
                extendedClient.log(sampleTransaction(UUID.randomUUID())));
        server.verify();
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
