package com.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.model.AuthorizationResponse;
import com.processing.support.CapturingAuthorizationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthorizationClientTest {

    private MockRestServiceServer mockServer;
    private AuthorizationClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AuthorizationClient(SwitchTestData.defaultProperties(), builder.build());
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void authorize_returnsApprovedResponse() throws Exception {
        AuthorizationResponse authResponse = CapturingAuthorizationClient.approvedFixture("000001");
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(authResponse), MediaType.APPLICATION_JSON));

        AuthorizationResponse response = client.authorize(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"));

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.responseCode()).isEqualTo("00");
    }

    @Test
    void authorize_retriesThreeTimesThenReturnsDeclined05() {
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AuthorizationResponse response = client.authorize(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"));

        assertThat(response.responseCode()).isEqualTo("05");
        assertThat(response.status()).isEqualTo("DECLINED");
    }

    @Test
    void reverse_sendsMti0400() throws Exception {
        AuthorizationResponse authResponse = CapturingAuthorizationClient.approvedFixture("000001");
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"mti\":\"0400\"}", false))
                .andRespond(withSuccess(objectMapper.writeValueAsString(authResponse), MediaType.APPLICATION_JSON));

        client.reverse(SwitchTestData.sampleRequest().withIssuerId("ISS001"));
    }

    @Test
    void checkHealth_whenAuthUp_returnsOk() {
        mockServer.expect(requestTo("http://localhost:8083/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(client.checkHealth()).isEqualTo("ok");
    }

    @Test
    void checkHealth_whenAuthDown_returnsDown() {
        mockServer.expect(requestTo("http://localhost:8083/health"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(client.checkHealth()).isEqualTo("down");
    }
}
