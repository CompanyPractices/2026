package com.processing.terminalsimulator.client;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RestClientTest
class GatewayClientTest {

    private MockWebServer mockServer;
    private GatewayClient gatewayClient;
    private String baseUrl;
    private String gatewayUrl;
    private String cardMgmtUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        baseUrl = mockServer.url("/").toString();

        gatewayUrl = baseUrl;
        cardMgmtUrl = baseUrl;
        RestClient restClient = RestClient.create();

        gatewayClient = new GatewayClient(restClient, gatewayUrl, cardMgmtUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void sendToGateway_shouldReturnAuthorizationResponse() throws InterruptedException {
        AuthorizationRequest request = new AuthorizationRequest("0100", "000001", "4000001234560001",
                "000000", new BigDecimal(10000L), "643",
                (LocalDateTime.of(2026, 6, 9, 10, 0, 0)).toInstant(ZoneOffset.UTC),
                "TERM001", "02", "MERCH001", "5411", "ACQ001", "");

        String responseJson = """
                {
                    "status": "APPROVED",
                    "code": "00",
                    "message": "OK",
                    "transactionId": "abc123"
                }
                """;

        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        AuthorizationResponse actual = gatewayClient.sendToGateway(request);
        assertEquals("APPROVED", actual.status());

        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(mockServer.url("/api/transactions").toString(), recordedRequest.getRequestUrl().toString());
    }

    @Test
    void getCardsFromCardManager_shouldReturnCards() throws InterruptedException {
        String responseJson = """
                {
                    "cards": [
                        {
                            "pan": "4000001234560001",
                            "status": "ACTIVE",
                            "currencyCode": "643",
                            "availableBalance": 500000,
                            "dailyLimit": 100000
                        }
                    ],
                    "total": 1
                }
                """;

        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        List<CardModel> cards = gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE, 70);
        assertEquals(1, cards.size());
        assertEquals("4000001234560001", cards.get(0).pan());

        RecordedRequest request = mockServer.takeRequest();
        assertEquals(mockServer.url("/api/cards?status=ACTIVE&limit=70").toString(), request.getRequestUrl().toString());
        assertEquals("GET", request.getMethod());
    }

    @Test
    void getCardsFromCardManager_whenResponseEmpty_shouldThrowException() {
        String responseJson = "{\"cards\":[], \"total\":0}";

        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE, 70))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ACTIVE cards available");
    }
}
