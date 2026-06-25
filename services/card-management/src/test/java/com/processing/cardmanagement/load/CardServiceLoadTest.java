package com.processing.cardmanagement.load;

import com.processing.cardmanagement.models.BinIssuerEntity;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardDraft;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.repositories.BinIssuerJpaRepository;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.BinIssuerService;
import com.processing.cardmanagement.services.CardService;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.common.dto.cardmanagement.PatchCardRequest;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardServiceLoadTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final int BINS_AMOUNT = 100;
    private static String[] bins;

    @Value("${local.server.port}")
    private int port;

    @Value("${app.card-service.tests.load.max-parallel-requests}")
    private int maximumParallelRequests;

    @Value("${app.card-service.tests.load.max-total-requests}")
    private int maximumTotalRequests;

    private LoadTestEngine loadTestEngine;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private CardService cardService;

    private static final ThreadLocal<Faker> faker = ThreadLocal.withInitial(() ->
        new Faker(ThreadLocalRandom.current())
    );

    @BeforeAll
    static void setUpBin(@Autowired BinIssuerJpaRepository binIssuerJpaRepository) {
        var binsIssuers = new ArrayList<BinIssuerEntity>(BINS_AMOUNT);
        for (int i = 0; i < BINS_AMOUNT; ++i) {
            binsIssuers.add(
                new BinIssuerEntity(
                    faker.get().number().digits(6),
                    faker.get().lorem().characters(6).toUpperCase(Locale.ROOT)
                )
            );
        }
        binIssuerJpaRepository.saveAll(binsIssuers);
        bins = binsIssuers.stream().map(BinIssuerEntity::getBin).toArray(String[]::new);
    }

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @Autowired
    private BinIssuerService binIssuerService;

    @BeforeEach
    void setUp() {
        this.loadTestEngine = new LoadTestEngine(
            Executors.newFixedThreadPool(maximumParallelRequests)
        );
    }

    @AfterEach
    void cleanUp() {
        try {
            cardJpaRepository.deleteAll();
        } catch (Exception ignored) {}
    }

    @Test
    void cardServiceCreateLoadTest() throws ExecutionException, InterruptedException {
        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                var postQuery = randomCreateCardRequest(faker.get());
                given()
                    .contentType(ContentType.JSON)
                    .body(postQuery)
                    .port(port)
                    .when()
                    .post("/api/cards")
                    .then()
                    .statusCode(201);
            }
        ).get();
    }

    @Test
    void cardServiceGetSingleCardLoadTest() throws ExecutionException, InterruptedException {
        var cardsAmount = 500;
        var pan = createRandomCards(cardsAmount)
            .stream()
            .map(Card::pan)
            .toList()
            .getFirst();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testGetCard(pan)
        ).get();
    }

    @Test
    void cardServiceGetManyCardsLoadTest() throws ExecutionException, InterruptedException {
        var cardsAmount = 1000;
        var pans = createRandomCards(cardsAmount)
            .stream()
            .map(Card::pan)
            .toArray(String[]::new);

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testGetCard(
                pans[ThreadLocalRandom.current().nextInt(0, cardsAmount)]
            )
        ).get();
    }

    @Test
    void cardServiceGetFilteredLoadTest() throws ExecutionException, InterruptedException {
        int cardsAmount = 1000;
        var cards = createRandomCards(cardsAmount);
        String[] bins = cards.stream().map(Card::bin).toArray(String[]::new);
        String[] issuerIds = cards.stream().map(Card::issuerId).toArray(String[]::new);
        CardModelStatus[] statuses = CardModelStatus.values();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();

                String randomBin = bins[random.nextInt(bins.length)];
                String randomIssuerId = issuerIds[random.nextInt(issuerIds.length)];
                CardModelStatus randomStatus = statuses[random.nextInt(statuses.length)];
                long randomOffset = random.nextLong(0, 500);

                given()
                    .queryParam("limit", 500)
                    .queryParam("offset", randomOffset)
                    .queryParam("status", randomStatus.name())
                    .queryParam("bin", randomBin)
                    .queryParam("issuerId", randomIssuerId)
                    .port(port)
                    .when()
                    .get("/api/cards")
                    .then()
                    .statusCode(200);
            }
        ).get();
    }

    @Test
    void cardServicePatchSingleCardLoadTest() throws ExecutionException, InterruptedException {
        int maxDailyLimit = 15_000_000;
        int maxMonthlyLimit = 300_000_000;
        int maxBalance = 1_000_000;

        var pan = createCard(randomCreateCardRequest(faker.get())).pan();
        var statuses = CardModelStatus.values();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testPatchCard(
                pan,
                statuses,
                maxDailyLimit,
                maxMonthlyLimit,
                maxBalance
            )
        ).get();
    }

    @Test
    void cardServicePatchManyCardsLoadTest() throws ExecutionException, InterruptedException {
        int cardsAmount = 500;
        int maxDailyLimit = 15_000_000;
        int maxMonthlyLimit = 300_000_000;
        int maxBalance = 1_000_000;

        var pans = createRandomCards(cardsAmount)
            .stream()
            .map(Card::pan)
            .toArray(String[]::new);
        var statuses = CardModelStatus.values();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testPatchCard(
                pans[ThreadLocalRandom.current().nextInt(0, cardsAmount)],
                statuses,
                maxDailyLimit,
                maxMonthlyLimit,
                maxBalance
            )
        ).get();
    }

    @Test
    void cardServiceDeleteLoadTest() throws ExecutionException, InterruptedException {
        int cardsAmount = 4000;
        var pansToDelete = createRandomCards(cardsAmount)
            .stream().map(Card::pan)
            .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                var pan = pansToDelete.poll();
                if (pan != null) {
                    given()
                        .pathParam("pan", pan)
                        .port(port)
                        .when()
                        .delete("/api/cards/{pan}")
                        .then()
                        .statusCode(204);
                }
            }
        ).get();
    }

    @Test
    void cardServiceReserveSingleCardLoadTest() throws ExecutionException, InterruptedException {
        var pan = createCard(randomCreateCardRequest(faker.get())).pan();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testReserveCard(pan, 1)
        ).get();
    }

    @Test
    void cardServiceReserveManyCardsLoadTest() throws ExecutionException, InterruptedException {
        var cardsAmount = 500;
        var pans = createRandomCards(cardsAmount)
            .stream()
            .map(Card::pan)
            .toArray(String[]::new);

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> testReserveCard(pans[ThreadLocalRandom.current().nextInt(0, cardsAmount)], 1)
        ).get();
    }

    @Test
    void cardServiceReserveAndRollbackManyCardsLoadTest() throws ExecutionException, InterruptedException {
        var cardsAmount = 500;
        var cards = new ConcurrentLinkedQueue<>(createRandomCards(cardsAmount));

        loadTestEngine.execute(
            maximumParallelRequests,
            Math.min(cardsAmount, maximumTotalRequests),
            () -> {
                var card = cards.poll();
                assertNotNull(card);
                var rrn = testReserveCard(card.pan(), card.availableBalance().longValue());
                var rollbackRequest = new RollbackRequest(
                    rrn,
                    card.pan(),
                    card.availableBalance()
                );
                given()
                    .contentType(ContentType.JSON)
                    .body(rollbackRequest)
                    .pathParam("pan", card.pan())
                    .port(port)
                    .when()
                    .post("/api/cards/{pan}/rollback")
                    .then()
                    .statusCode(200);
            }
        ).get();
    }

    private void testGetCard(String pan) {
        given()
            .pathParam("PAN", pan)
            .port(port)
            .when()
            .get("/api/cards/{PAN}")
            .then()
            .statusCode(200);
    }

    private void testPatchCard(
        String pan,
        CardModelStatus[] statuses,
        int maxDailyLimit,
        int maxMonthlyLimit,
        int maxBalance
    ) {
        var random = ThreadLocalRandom.current();

        var patchRequest = new PatchCardRequest(
            statuses[random.nextInt(statuses.length)],
            BigDecimal.valueOf(random.nextLong(0, maxDailyLimit)),
            BigDecimal.valueOf(random.nextLong(maxDailyLimit, maxMonthlyLimit)),
            BigDecimal.valueOf(random.nextLong(0, maxBalance))
        );

        given()
            .contentType(ContentType.JSON)
            .body(patchRequest)
            .pathParam("pan", pan)
            .port(port)
            .when()
            .patch("/api/cards/{pan}")
            .then()
            .statusCode(200);
    }

    private String testReserveCard(String pan, long maxAmount) {
        var f = faker.get();

        var reserveRequest = new ReserveRequest(
            BigDecimal.valueOf(f.number().numberBetween(1, maxAmount)),
            f.number().digits(12)
        );

        given()
            .contentType(ContentType.JSON)
            .body(reserveRequest)
            .pathParam("pan", pan)
            .port(port)
            .when()
            .post("/api/cards/{pan}/reserve")
            .then()
            .statusCode(200);

        return reserveRequest.rrn();
    }

    private List<Card> createRandomCards(int value) {
        var f = faker.get();
        var drafts = new ArrayList<CardDraft>(value);

        for (int i = 0; i < value; ++i) {
            var dailyLimit = f.number().numberBetween(0, 15_000_000);
            drafts.add(
                new CardDraft(
                    f.options().option(bins),
                    f.name().fullName().toUpperCase(Locale.ROOT),
                    f.options().option(CardStatus.ACTIVE),
                    f.number().digits(3),
                    BigDecimal.valueOf(f.number().numberBetween(0, dailyLimit)),
                    BigDecimal.valueOf(f.number().numberBetween(dailyLimit, 300_000_000)),
                    BigDecimal.valueOf(f.number().numberBetween(100_000, 1_000_000))
                )
            );
        }
        return cardService.createCards(drafts);
    }

    private CreateCardRequest randomCreateCardRequest(Faker faker) {
        var dailyLimit = faker.number().numberBetween(0L, 15_000_000L);
        return new CreateCardRequest(
            faker.options().option(bins),
            faker.name().fullName().toUpperCase(Locale.ROOT),
            faker.number().digits(3),
            BigDecimal.valueOf(dailyLimit),
            BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000L)),
            BigDecimal.valueOf(faker.number().numberBetween(100_000L, 1_000_000L))
        );
    }

    private Card createCard(CreateCardRequest req) {
        return given()
            .contentType(ContentType.JSON)
            .body(req)
            .port(port)
            .when()
            .post("/api/cards")
            .then()
            .statusCode(201)
            .extract()
            .as(Card.class);
    }
}
