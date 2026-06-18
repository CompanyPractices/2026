package com.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.config.RetryFactory;
import com.processing.config.SwitchProperties;
import com.processing.exception.AuthorizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit-тесты HTTP-клиента {@link AuthorizationClient} (authorize, rollback, health).
 */
class AuthorizationClientTest {

    private static final String ROLLBACK_URL = "http://localhost:8083/api/internal/rollback";
    private static final String TEST_RRN = "012345678901";
    private static final String TEST_PAN = "4000001234560001";
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(150000);

    private MockRestServiceServer mockServer;
    private AuthorizationClient client;
    private ObjectMapper objectMapper;

    /** Настраивает MockRestServiceServer и клиент. */
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

    /** Проверяет, что все ожидаемые HTTP-вызовы были выполнены. */
    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    /** Успешный authorize → APPROVED и корректный RRN. */
    @Test
    void authorize_whenAuthReturnsApproved_returnsResponse() throws Exception {
        AuthorizationResponse authResponse = new AuthorizationResponse(
                "0110", "000001", TEST_RRN, "ABC123",
                AuthorizationResponse.CODE_APPROVED,
                AuthorizationResponse.STATUS_APPROVED,
                null, 42);
        mockServer.expect(requestTo("http://localhost:8083/api/internal/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(authResponse), MediaType.APPLICATION_JSON));

        AuthorizationResponse response = client.authorize(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"));

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(response.rrn()).isEqualTo(TEST_RRN);
    }

    /** Недоступный Authorization после retry → {@link AuthorizationException}. */
    @Test
    void authorize_whenAuthUnreachableAfterRetries_throwsAuthorizationException() {
        SwitchProperties properties = new SwitchProperties(
                "1.0.0",
                SwitchTestData.BIN_ROUTING,
                "http://127.0.0.1:1",
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

    /** Rollback передаёт rrn, pan и amount в теле запроса. */
    @Test
    void rollback_sendsRrnPanAndAmount() throws Exception {
        RollbackResponse approved = RollbackResponse.approved(new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.rrn").value(TEST_RRN))
                .andExpect(jsonPath("$.pan").value("4000001234560001"))
                .andExpect(jsonPath("$.amount").value(150000))
                .andRespond(withSuccess(objectMapper.writeValueAsString(approved), MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_SUCCESS);
    }

    /** Успешный rollback → код {@code 00}. */
    @Test
    void rollback_whenApproved_returnsCode00() throws Exception {
        RollbackResponse approved = RollbackResponse.approved(new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(approved), MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_SUCCESS);
        assertThat(response.declineReason()).isNull();
    }

    /** Транзакция не найдена → HTTP 404, код {@code 14}. */
    @Test
    void rollback_whenTransactionNotFound_returnsCode14() throws Exception {
        RollbackResponse declined = RollbackResponse.declined(
                new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "TRANSACTION_NOT_FOUND",
                RollbackResponse.CODE_TRANSACTION_NOT_FOUND, Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body(objectMapper.writeValueAsString(declined))
                        .contentType(MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_TRANSACTION_NOT_FOUND);
        assertThat(response.declineReason()).isEqualTo("TRANSACTION_NOT_FOUND");
    }

    /** Повторный rollback → HTTP 409, код {@code 05}. */
    @Test
    void rollback_whenAlreadyRolledBack_returnsCode05() throws Exception {
        RollbackResponse declined = RollbackResponse.declined(
                new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "ALREADY_ROLLED_BACK",
                RollbackResponse.CODE_DECLINED_GENERAL, Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .body(objectMapper.writeValueAsString(declined))
                        .contentType(MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_DECLINED_GENERAL);
        assertThat(response.declineReason()).isEqualTo("ALREADY_ROLLED_BACK");
    }

    /** Ошибка rollback на стороне Auth → код {@code 96}. */
    @Test
    void rollback_whenRollbackFailed_returnsCode96() throws Exception {
        RollbackResponse declined = RollbackResponse.declined(
                new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "ROLLBACK_FAILED",
                RollbackResponse.CODE_SERVICE_UNAVAILABLE, Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(objectMapper.writeValueAsString(declined))
                        .contentType(MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_SERVICE_UNAVAILABLE);
        assertThat(response.declineReason()).isEqualTo("ROLLBACK_FAILED");
    }

    /** Authorization недоступен при rollback → код {@code 96}. */
    @Test
    void rollback_whenServiceUnavailable_returnsCode96() throws Exception {
        RollbackResponse declined = RollbackResponse.declined(
                new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "SERVICE_UNAVAILABLE",
                RollbackResponse.CODE_SERVICE_UNAVAILABLE, Instant.now());
        mockServer.expect(requestTo(ROLLBACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(objectMapper.writeValueAsString(declined))
                        .contentType(MediaType.APPLICATION_JSON));

        RollbackResponse response = client.rollback(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"), TEST_RRN);

        assertThat(response.status()).isEqualTo(RollbackResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(RollbackResponse.CODE_SERVICE_UNAVAILABLE);
        assertThat(response.declineReason()).isEqualTo("SERVICE_UNAVAILABLE");
    }

    /** Health-check Authorization → {@code "ok"}. */
    @Test
    void checkHealth_whenAuthUp_returnsOk() {
        mockServer.expect(requestTo("http://localhost:8083/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(client.checkHealth()).isEqualTo("ok");
    }
}
