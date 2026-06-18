package com.processing.cardmanagement.load;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.CardService;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.common.dto.cardmanagement.PatchCardRequest;
import com.processing.common.dto.cardmanagement.ReserveRequest;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardServiceLoadTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

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

    private final ThreadLocal<Faker> faker = ThreadLocal.withInitial(() ->
            new Faker(ThreadLocalRandom.current())
    );

    @Autowired
    private CardService cardService;

    @Autowired
    private CardServiceSettings settings;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @BeforeEach
    void setUp() {
        this.loadTestEngine = new LoadTestEngine(
                Executors.newFixedThreadPool(maximumParallelRequests)
        );
    }

    @AfterEach
    void cleanUp() {
        cardJpaRepository.deleteAll();
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
        var cardsAmount = 500;
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
        int cardsAmount = settings.maxPageLimit() * 2;
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
                    long randomOffset = random.nextLong(0, settings.maxPageLimit());

                    given()
                            .queryParam("limit", settings.maxPageLimit())
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
        var pansToDelete = new ConcurrentLinkedQueue<String>();

        for (int i = 0; i < maximumTotalRequests; i++) {
            Card c = createCard(randomCreateCardRequest(faker.get()));
            pansToDelete.add(c.pan());
        }

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
        var maxReservationAmount = 1_000_000;
        var pans = createRandomCards(cardsAmount)
                .stream()
                .map(Card::pan)
                .toArray(String[]::new);

        loadTestEngine.execute(
                maximumParallelRequests,
                maximumTotalRequests,
                () -> testReserveCard(
                        pans[ThreadLocalRandom.current().nextInt(0, cardsAmount)],
                        maxReservationAmount
                )
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

    private void testReserveCard(String pan, long maxAmount) {
        var f = faker.get();

        var reserveRequest = new ReserveRequest(
                BigDecimal.valueOf(f.number().numberBetween(0, maxAmount)),
                f.lorem().characters()
        );

        given()
                .contentType(ContentType.JSON)
                .body(reserveRequest)
                .pathParam("pan", pan)
                .port(port)
                .when()
                .patch("/api/cards/{pan}")
                .then()
                .statusCode(anyOf(is(200), is(422)));
    }

    private List<Card> createRandomCards(int value) {
        var pans = new ArrayList<Card>(value);
        for (int i = 0; i < value; ++i) {
            pans.add(createCard(randomCreateCardRequest(faker.get())));
        }
        return pans;
    }

    private CreateCardRequest randomCreateCardRequest(Faker faker) {
        var dailyLimit = faker.number().numberBetween(0L, 15_000_000L);
        return new CreateCardRequest(
                faker.number().digits(6),
                faker.name().fullName().toUpperCase(Locale.ROOT),
                faker.number().digits(3),
                BigDecimal.valueOf(dailyLimit),
                BigDecimal.valueOf(faker.number().numberBetween(dailyLimit, 300_000_000L)),
                BigDecimal.valueOf(faker.number().numberBetween(0L, 1_000_000L))
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
