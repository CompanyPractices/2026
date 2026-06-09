package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@AutoConfigureMockMvc
@Testcontainers
public class CardGeneratorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

//    @AfterEach
//    void cleanUp() {
//        cardRepository.
//    }

    @Test
    void generateShouldSaveCardsToDatabaseAndReturn201() throws Exception {
        int count = 10;
        List<String> bins = List.of("400000", "400001");
        GenerateCardsRequest request = new GenerateCardsRequest(count, bins);

        mockMvc.perform(post("/api/cards/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generated").value(10))
                .andExpect(jsonPath("$.cards", hasSize(10)));

        long dbCount = cardRepository.countCards();
        assertEquals(2, dbCount);
    }

    @Test
    void generateShouldReturn400WhenCountIsZero() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(0, List.of("400000"));

        mockMvc.perform(post("/api/cards/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
