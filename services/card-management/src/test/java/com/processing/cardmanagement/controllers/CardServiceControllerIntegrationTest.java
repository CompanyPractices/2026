package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.BinIssuerService;
import com.processing.common.dto.cardmanagement.*;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardServiceControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Value("${local.server.port}")
    private int port;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private final static Faker faker = new Faker();

    @Autowired
    private BinIssuerService binIssuerService;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @AfterEach
    void cleanUp() {
        cardJpaRepository.deleteAll();
    }

    @Test
    void cardServiceShouldCreateValidCard() {
        var postQuery = createRandomValidCreationRequest();

        given()
            .contentType(ContentType.JSON)
            .body(postQuery)
            .port(port)
            .when()
            .post("/api/cards")
            .then()
            .statusCode(201)
            .body("pan", startsWith(postQuery.bin()))
            .body("bin", equalTo(postQuery.bin()))
            .body("cardholderName", equalTo(postQuery.cardholderName()))
            .body("dailyLimit", equalTo(postQuery.dailyLimit().intValue()))
            .body("monthlyLimit", equalTo(postQuery.monthlyLimit().intValue()))
            .body("availableBalance", equalTo(postQuery.initialBalance().intValue()));

        assertEquals(1, cardJpaRepository.count());
    }

    @Test
    void cardServiceShouldNotCreateCardWithInvalidBin() {
        var cardholderName = faker.name().fullName().toUpperCase(Locale.ROOT);
        var currencyCode = faker.number().digits(3);
        var dailyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(0, 15_000_000)
        );
        var monthlyLimit = BigDecimal.valueOf(
            faker.number().numberBetween(dailyLimit.intValue(), 300_000_000)
        );
        var initialBalance = BigDecimal.valueOf(
            faker.number().numberBetween(0, 1_000_000)
        );

        var request = new CreateCardRequest(
            faker.number().digits(5),
            cardholderName,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            initialBalance
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .port(port)
            .when()
            .post("/api/cards")
            .then()
            .statusCode(400);

        assertEquals(0, cardJpaRepository.count());

        request = new CreateCardRequest(
            faker.lorem().characters(6),
            cardholderName,
            currencyCode,
            dailyLimit,
            monthlyLimit,
            initialBalance
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .port(port)
            .when()
            .post("/api/cards")
            .then()
            .statusCode(400);

        assertEquals(0, cardJpaRepository.count());
    }

    @Test
    void cardServiceShouldGetSavedCards() {
        int limit = 4;
        int cardAmount = 5;
        for (int i = 0; i < cardAmount; ++i) {
            createRandomCard(createRandomValidCreationRequest());
        }

        var jsonPath = given()
            .port(port)
            .queryParam("limit", limit)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

        assertEquals(cardAmount, jsonPath.getInt("total"));
        assertEquals(limit, jsonPath.getList("cards").size());
    }

    @Test
    void cardServiceShouldGetSavedCard() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();

        given()
            .port(port)
            .pathParam("PAN", pan)
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(200)
            .body("pan", equalTo(pan));
    }

    @Test
    void cardServiceShouldNotGetNonExistingCard() {
        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(16))
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(404);
    }

    @Test
    void cardServiceShouldNotGetInvalidPan() {
        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(15))
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(400);

        given()
            .port(port)
            .pathParam("PAN", faker.lorem().characters(16))
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(400);
    }

    @Test
    void cardServiceShouldPathValidCardWithValidData() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();
        var dailyLimit = faker.number().numberBetween(0, 15_000_000);
        var patchRequest = new PatchCardRequest(

            faker.random().nextEnum(CardModelStatus.class),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000))
        );
        assertNotNull(patchRequest.status());
        assertNotNull(patchRequest.dailyLimit());
        assertNotNull(patchRequest.monthlyLimit());
        assertNotNull(patchRequest.availableBalance());

        given()
            .port(port)
            .pathParam("PAN", pan)
            .contentType(ContentType.JSON)
            .body(patchRequest)
            .when()
            .patch("/api/cards/{PAN}")
            .then()
            .statusCode(200)
            .body("status", equalTo(patchRequest.status().name()))
            .body("dailyLimit", equalTo(patchRequest.dailyLimit().intValue()))
            .body("monthlyLimit", equalTo(patchRequest.monthlyLimit().intValue()))
            .body("availableBalance", equalTo(patchRequest.availableBalance().intValue()));

    }

    @Test
    void cardServiceShouldReserveMoneyIfBalanceIsEnough() {
        var card = createRandomCard(createRandomValidCreationRequest());
        var amount = card.availableBalance();
        var reserveRequest = new ReserveRequest(
            amount,
            faker.number().digits(12)
        );

        given()
            .port(port)
            .pathParam("PAN", card.pan())
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .when()
            .post("/api/cards/{PAN}/reserve")
            .then()
            .statusCode(200);
    }

    @Test
    void cardServiceCantReserveNegativeMoney() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();
        var amount = -1;
        // negative amount validator is in the ReserveRequest itself
        var reserveRequest = Map.of(
            "amount", amount,
            "rrn", faker.number().digits(12)
        );

        given()
            .port(port)
            .pathParam("PAN", pan)
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .when()
            .post("/api/cards/{PAN}/reserve")
            .then()
            .statusCode(400);

        reserveRequest = Map.of(
            "amount", faker.number().numberBetween(0, 1_000_000),
            "rrn", faker.lorem().characters()
        );

        given()
            .port(port)
            .pathParam("PAN", pan)
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .when()
            .post("/api/cards/{PAN}/reserve")
            .then()
            .statusCode(400);
    }

    @Test
    void cardServiceCantReserveMoreMoneyThanOnTheBalanceItself() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();
        var amount = BigDecimal.valueOf(Long.MAX_VALUE);
        var reserveRequest = new ReserveRequest(
            amount,
            faker.number().digits(12)
        );

        given()
            .port(port)
            .pathParam("PAN", pan)
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .when()
            .post("/api/cards/{PAN}/reserve")
            .then()
            .statusCode(402);
    }

    @Test
    void cardServiceShouldDeleteCard() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();

        given()
            .port(port)
            .pathParam("PAN", pan)
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(204);

        given()
            .port(port)
            .pathParam("PAN", pan)
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(404);
    }

    @Test
    void cardServiceShouldDeleteCardOnlyOnce() {
        var pan = createRandomCard(createRandomValidCreationRequest()).pan();

        given()
            .port(port)
            .pathParam("PAN", pan)
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(204);

        given()
            .port(port)
            .pathParam("PAN", pan)
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(404);
    }

    @Test
    void cardServiceShouldNotDeleteNonExistentCard() {
        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(16))
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(404);
    }

    private CreateCardRequest createRandomValidCreationRequest() {
        var dailyLimit = faker.number().numberBetween(0, 15_000_000);
        var bins = binIssuerService
            .getAll()
            .stream()
            .map(BinIssuer::bin)
            .toArray(String[]::new);

        return new CreateCardRequest(
            faker.options().option(bins),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            faker.number().digits(3),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000)),
            BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000))
        );
    }

    private CardModel createRandomCard(CreateCardRequest postQuery) {
        return given()
            .contentType(ContentType.JSON)
            .body(postQuery)
            .port(port)
            .when()
            .post("/api/cards")
            .then()
            .statusCode(201)
            .extract()
            .as(CardModel.class);
    }
}
