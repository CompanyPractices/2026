package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CardGeneratorControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Value("${local.server.port}")
    private int port;

    @AfterEach
    void cleanUp() {
        cardJpaRepository.deleteAll();
    }

    @Test
    void generateShouldSaveCardsToDatabaseAndReturn201() throws Exception {
        int count = 10;
        List<String> bins = List.of("400000", "400001");
        GenerateCardsRequest request = new GenerateCardsRequest(count, bins);

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(201)
                .body("generated", equalTo(10))
                .body("cards", hasSize(10));

        long dbCount = cardJpaRepository.count();
        assertEquals(10, dbCount);
    }

    @Test
    void generateShouldReturn400WhenCountIsZero() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(0, List.of("400000"));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .port(port)
                .when()
                .post("/api/cards/generate")
                .then()
                .statusCode(400);
    }
}
