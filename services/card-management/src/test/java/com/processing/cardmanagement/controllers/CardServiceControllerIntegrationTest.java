package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.common.dto.cardmanagement.*;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardServiceControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Value("${local.server.port}")
    private int port;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private final Faker faker = new Faker();

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
            .body("dailyLimit", equalTo((int) postQuery.dailyLimit()))
            .body("monthlyLimit", equalTo((int) postQuery.monthlyLimit()))
            .body("availableBalance", equalTo((int) postQuery.initialBalance()));

        assertEquals(1, cardJpaRepository.count());
    }

    @Test
    void cardServiceShouldNotCreateCardWithInvalidBin() {
        var cardholderName = faker.name().fullName().toUpperCase(Locale.ROOT);
        var currencyCode = faker.number().digits(3);
        var dailyLimit = faker.number().numberBetween(0, 15_000_000);
        var monthlyLimit = faker.number().numberBetween(dailyLimit, 300_000_000);
        var initialBalance = faker.number().numberBetween(0, 1_000_000);

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
        var dailyLimit = faker.number().numberBetween(0L, 15_000_000L);
        var patchRequest = new PatchCardRequest(
            faker.random().nextEnum(CardModelStatus.class),
            dailyLimit,
            faker.number().numberBetween(dailyLimit, 300_000_000L),
            faker.number().numberBetween(0L, 1_000_000L)
        );
        Assertions.assertNotNull(patchRequest.status());
        Assertions.assertNotNull(patchRequest.dailyLimit());
        Assertions.assertNotNull(patchRequest.monthlyLimit());
        Assertions.assertNotNull(patchRequest.availableBalance());

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
            faker.lorem().characters()
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
            "description", faker.lorem().characters()
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
        var amount = Long.MAX_VALUE;
        var reserveRequest = new ReserveRequest(
            amount,
            faker.lorem().characters()
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
            .contentType(ContentType.JSON)
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(204);
    }

    @Test
    void cardServiceShouldDeleteNonExistentCard() {
        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(16))
            .contentType(ContentType.JSON)
            .when()
            .delete("/api/cards/{PAN}")
            .then()
            .statusCode(404);
    }

    private CreateCardRequest createRandomValidCreationRequest() {
        var dailyLimit = faker.number().numberBetween(0L, 15_000_000L);
        return new CreateCardRequest(
            faker.number().digits(6),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            faker.number().digits(3),
            dailyLimit,
            faker.number().numberBetween(dailyLimit, 300_000_000L),
            faker.number().numberBetween(0L, 1_000_000L)
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
