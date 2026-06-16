package com.processing.cardmanagement.load;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.services.CardService;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.CreateCardRequest;
import com.processing.common.dto.cardmanagement.PatchCardRequest;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private final ThreadLocal<Faker> threadLocalFaker = ThreadLocal.withInitial(Faker::new);

    @Autowired
    private CardService cardService;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @BeforeEach
    void setUp() {
        cardJpaRepository.deleteAll();

        this.loadTestEngine = new LoadTestEngine(
            Executors.newFixedThreadPool(maximumParallelRequests)
        );
    }

    @Test
    void cardServiceCreateLoadTest() throws ExecutionException, InterruptedException {
        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                var postQuery = randomCreateCardRequest(threadLocalFaker.get());
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
        assertEquals(maximumTotalRequests, cardService.countAllCards());
    }

    @Test
    void cardServiceGetLoadTest() throws ExecutionException, InterruptedException {
        Faker faker = threadLocalFaker.get();
        Card seededCard = createCard(randomCreateCardRequest(faker));
        String pan = seededCard.pan();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> given()
                .pathParam("PAN", pan)
                .port(port)
                .when()
                .get("/api/cards/{PAN}")
                .then()
                .statusCode(200)
        ).get();
    }

    @Test
    void cardServiceGetFilteredLoadTest() throws ExecutionException, InterruptedException {
        int seedCount = 100;
        Faker faker = threadLocalFaker.get();
        List<String> binList = new ArrayList<>();
        List<String> issuerIdList = new ArrayList<>();

        for (int i = 0; i < seedCount; i++) {
            Card c = createCard(randomCreateCardRequest(faker));
            binList.add(c.bin());
            issuerIdList.add(c.issuerId());
        }

        String[] bins = binList.toArray(new String[0]);
        String[] issuerIds = issuerIdList.toArray(new String[0]);
        CardModelStatus[] statuses = CardModelStatus.values();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();

                String randomBin = bins[random.nextInt(bins.length)];
                String randomIssuerId = issuerIds[random.nextInt(issuerIds.length)];
                CardModelStatus randomStatus = statuses[random.nextInt(statuses.length)];
                int randomOffset = random.nextInt(0, 50);

                given()
                    .queryParam("limit", seedCount)
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
    void cardServicePatchLoadTest() throws ExecutionException, InterruptedException {
        int poolSize = 500;
        int maxDailyLimit = 15_000_000;
        int maxMonthlyLimit = 300_000_000;
        int maxBalance = 1_000_000;
        var faker = threadLocalFaker.get();
        var panPool = new ArrayList<String>();

        for (int i = 0; i < poolSize; i++) {
            Card c = createCard(randomCreateCardRequest(faker));
            panPool.add(c.pan());
        }

        var pans = panPool.toArray(String[]::new);
        var statuses = CardModelStatus.values();

        loadTestEngine.execute(
            maximumParallelRequests,
            maximumTotalRequests,
            () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                String randomPan = pans[random.nextInt(pans.length)];
                CardModelStatus randomStatus = statuses[random.nextInt(statuses.length)];

                var patchRequest = new PatchCardRequest(
                    randomStatus,
                    random.nextLong(0, maxDailyLimit),
                    random.nextLong(maxDailyLimit, maxMonthlyLimit),
                    random.nextLong(0, maxBalance)
                );

                given()
                    .contentType(ContentType.JSON)
                    .body(patchRequest)
                    .pathParam("pan", randomPan)
                    .port(port)
                    .when()
                    .patch("/api/cards/{pan}")
                    .then()
                    .statusCode(200);
            }
        ).get();
    }

    @Test
    void cardServiceDeleteLoadTest() throws ExecutionException, InterruptedException {
        Faker faker = threadLocalFaker.get();
        var pansToDelete = new ConcurrentLinkedQueue<String>();

        for (int i = 0; i < maximumTotalRequests; i++) {
            Card c = createCard(randomCreateCardRequest(faker));
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
        assertEquals(0, pansToDelete.size());
    }


    private CreateCardRequest randomCreateCardRequest(Faker faker) {
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
