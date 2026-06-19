package com.processing.cardmanagement.controllers;

import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.BinIssuerService;
import com.processing.cardmanagement.services.CardService;
import com.processing.common.dto.authorization.RollbackRequest;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

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

    @Autowired
    private CardService cardService;

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
    void cardServiceGetShouldGetFilteredResults() {
        var cardsAmount = 10;
        var statuses = CardModelStatus.values();
        var cards = Stream.generate(this::createRandomValidCreationRequest)
            .limit(cardsAmount)
            .map(request -> new CardDraft(
                request.bin(),
                request.cardholderName(),
                CardStatus.valueOf(
                    faker.options().option(statuses).name()
                ),
                request.currencyCode(),
                request.dailyLimit(),
                request.monthlyLimit(),
                request.initialBalance()
            ))
            .toList();

        cardService.createCards(cards);

        var limit = 9;
        var jsonPath = given()
            .port(port)
            .queryParam("limit", limit)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
        assertEquals(limit, jsonPath.getList("cards").size());

        var offset = 9;
        jsonPath = given()
            .port(port)
            .queryParam("offset", offset)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
        assertEquals(cardsAmount - offset, jsonPath.getList("cards").size());

        var status = CardModelStatus.ACTIVE;
        var gotCards = given()
            .port(port)
            .queryParam("status", status)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .as(GetCardsResponse.class);
        for (var c : gotCards.cards()) {
            assertEquals(status, c.status());
        }

        var bin = binIssuerService.getAll().getFirst().bin();
        gotCards = given()
            .port(port)
            .queryParam("bin", bin)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .as(GetCardsResponse.class);
        for (var c : gotCards.cards()) {
            assertEquals(bin, c.bin());
        }

        var issuerId = binIssuerService.getAll().getFirst().issuerId();
        gotCards = given()
            .port(port)
            .queryParam("issuerId", issuerId)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .as(GetCardsResponse.class);
        for (var c : gotCards.cards()) {
            assertEquals(issuerId, c.issuerId());
        }

        var startDate = Instant.now().minus(1, ChronoUnit.HOURS).toString();
        var endDate = Instant.now().plus(1, ChronoUnit.HOURS).toString();
        jsonPath = given()
            .port(port)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
        assertEquals(cardsAmount, jsonPath.getList("cards").size());

        jsonPath = given()
            .port(port)
            .queryParam("endDate", startDate)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
        assertEquals(0, jsonPath.getList("cards").size());

        jsonPath = given()
            .port(port)
            .queryParam("startDate", endDate)
            .when()
            .get("/api/cards")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
        assertEquals(0, jsonPath.getList("cards").size());
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
    void cardServiceShouldRollback() {
        var rrn = faker.number().digits(12);
        var card = createCardAndReserveAllMoney(rrn);
        var amount = BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000));
        var rollbackRequest = new RollbackRequest(
            rrn,
            card.pan(),
            amount
        );

        var model = given()
            .port(port)
            .pathParam("PAN", card.pan())
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(200)
            .extract()
            .as(CardModel.class);

        assertEquals(amount, model.availableBalance().setScale(0, RoundingMode.HALF_EVEN));
    }

    @Test
    void cardServiceShouldNotRollbackIfReservationDoesNotExist() {
        var rrn = faker.number().digits(12);
        var card = createRandomCard(createRandomValidCreationRequest());
        var amount = BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000));
        var rollbackRequest = new RollbackRequest(
            rrn,
            card.pan(),
            amount
        );

        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(16))
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(404);
    }

    @Test
    void cardServiceShouldNotRollbackIfWrongPanPassed() {
        var rrn = faker.number().digits(12);
        createCardAndReserveAllMoney(rrn);
        var wrongCard = createRandomCard(createRandomValidCreationRequest());
        var amount = BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000));
        var rollbackRequest = new RollbackRequest(
            rrn,
            wrongCard.pan(),
            amount
        );

        given()
            .port(port)
            .pathParam("PAN", faker.number().digits(16))
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(404);

        given()
            .port(port)
            .pathParam("PAN", wrongCard.pan())
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(400);
    }

    @Test
    void cardServiceShouldNotRollbackIfRollbackAlreadySatisfied() {
        var rrn = faker.number().digits(12);
        var card = createCardAndReserveAllMoney(rrn);
        var amount = BigDecimal.valueOf(faker.number().numberBetween(0, 1_000_000));
        var rollbackRequest = new RollbackRequest(
            rrn,
            card.pan(),
            amount
        );

        given()
            .port(port)
            .pathParam("PAN", card.pan())
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(200);

        given()
            .port(port)
            .pathParam("PAN", card.pan())
            .contentType(ContentType.JSON)
            .body(rollbackRequest)
            .when()
            .post("/api/cards/{PAN}/rollback")
            .then()
            .statusCode(409);
    }

    private CardModel createCardAndReserveAllMoney(String rrn) {
        var card = createRandomCard(createRandomValidCreationRequest());
        var reserveRequest = new ReserveRequest(
            card.availableBalance(),
            rrn
        );

        var res = given()
            .port(port)
            .pathParam("PAN", card.pan())
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .when()
            .post("/api/cards/{PAN}/reserve")
            .then()
            .statusCode(200)
            .extract()
            .as(CardModel.class);

        assertEquals(BigDecimal.ZERO, res.availableBalance().setScale(0, RoundingMode.HALF_DOWN));
        return res;
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
