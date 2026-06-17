package com.processing.cardmanagement.load;

import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardGeneratorServiceLoadTest {

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
    void cardGenerationConcurrentLoadTest() throws ExecutionException, InterruptedException {
        int count = 1;
        int beansAmount = 100;
        int totalRequests = 5000;

        loadTestEngine.execute(
            maximumParallelRequests,
            totalRequests,
            () -> testCardGeneration(count, beansAmount)
        ).get();
    }

    @Test
    void cardGenerationManyLargeDataLoadTest() throws ExecutionException, InterruptedException {
        int count = 1000;
        int beansAmount = 100;
        int totalRequests = 10;

        loadTestEngine.execute(
            Math.min(maximumParallelRequests, totalRequests),
            totalRequests,
            () -> testCardGeneration(count, beansAmount)
        ).get();
    }

    @Test
    void cardGenerationSingleLargeDataLoadTest() {
        int count = 10000;
        int beansAmount = 100;
        testCardGeneration(count, beansAmount);
    }

    private void testCardGeneration(int count, int beansAmount) {
        var f = faker.get();
        var bins = new ArrayList<String>(beansAmount);
        for (int i = 0; i < beansAmount; i++) {
            bins.add(f.number().digits(6));
        }
        var request = new GenerateCardsRequest(count, bins);
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .port(port)
            .when()
            .post("/api/cards/generate")
            .then()
            .statusCode(201);
    }
}
