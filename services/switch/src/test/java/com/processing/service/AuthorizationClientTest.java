package com.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.config.RetryFactory;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import com.processing.model.AuthorizationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthorizationClientTest {

    private MockRestServiceServer mockServer;
    private AuthorizationClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AuthorizationClient(
                SwitchTestData.defaultProperties(),
                builder.build(),
                RetryFactory.authorizationRetry(SwitchTestData.defaultProperties()));
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void authorize_whenAuthReturnsApproved_returnsResponse() throws Exception {
        AuthorizationResponse authResponse = new AuthorizationResponse(
                "0110", "000001", "012345678901", "ABC123", "00", "APPROVED", null, 42);
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(authResponse), MediaType.APPLICATION_JSON));

        AuthorizationResponse response = client.authorize(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"));

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(response.rrn()).isEqualTo("012345678901");
    }

    @Test
    void authorize_whenAuthUnreachableAfterRetries_throwsAuthorizationException() {
        SwitchProperties properties = new SwitchProperties(
                "1.0.0",
                SwitchTestData.BIN_ROUTING,
                "http://127.0.0.1:1",
                "http://127.0.0.1:1",
                SwitchTestData.defaultHttp(),
                SwitchTestData.defaultRetry()
        );
        AuthorizationClient unreachableClient = new AuthorizationClient(
                properties, RestClient.create(), RetryFactory.authorizationRetry(properties));

        assertThrows(AuthorizationException.class, () ->
                unreachableClient.authorize(SwitchTestData.sampleRequest().withIssuerId("ISS001")));
    }

    @Test
    void reverse_sendsMti0400WithRrn() {
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.mti").value("0400"))
                .andExpect(jsonPath("$.rrn").value("012345678901"))
                .andExpect(jsonPath("$.stan").value("000001"))
                .andRespond(withSuccess());

        client.reverse(SwitchTestData.sampleRequest().withIssuerId("ISS001"), "012345678901");
    }

    @Test
    void checkHealth_whenAuthUp_returnsOk() {
        mockServer.expect(requestTo("http://localhost:8083/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(client.checkHealth()).isEqualTo("ok");
    }
}
