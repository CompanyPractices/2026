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
        int count = 100;
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
                .extract()
                .jsonPath();
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

    @Test
    void generateShouldReturn400WhenBinsInvalid() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(1, List.of("ABCDEF"));

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
