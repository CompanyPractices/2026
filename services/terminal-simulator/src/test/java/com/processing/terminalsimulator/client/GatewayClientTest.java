package com.processing.terminalsimulator.client;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.terminalsimulator.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GatewayClientTest {

    private GatewayClient gatewayClient;
    private MockRestServiceServer mockServer;
    private static final String GATEWAY_URL = "http://localhost:8080";
    private static final String CARD_MGMT_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        gatewayClient = new GatewayClient(restTemplate, GATEWAY_URL, CARD_MGMT_URL);
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void sendToGateway_shouldReturnAuthorizationResponse() {
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

        mockServer.expect(requestTo(GATEWAY_URL + "/api/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"stan\":\"000001\"}"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AuthorizationResponse response = gatewayClient.sendToGateway(request);
        assertEquals(TransactionStatus.APPROVED.name(), response.status());
        mockServer.verify();
    }

    @Test
    void getCardsFromCardManager_shouldReturnCards() {
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

        mockServer.expect(requestTo(CARD_MGMT_URL + "/api/cards?status=ACTIVE&limit=70"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CardModel> cards = gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE, 70);
        assertEquals(1, cards.size());
        assertEquals("4000001234560001", cards.get(0).pan());
    }

    @Test
    void getCardsFromCardManager_whenResponseEmpty_shouldThrowException() {
        String responseJson = "{\"cards\":[], \"total\":0}";

        mockServer.expect(requestTo(CARD_MGMT_URL + "/api/cards?status=ACTIVE&limit=70"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE, 70))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ACTIVE cards available");
    }
}
