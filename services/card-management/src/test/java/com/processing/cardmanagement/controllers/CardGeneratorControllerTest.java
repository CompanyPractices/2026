package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.mappers.CardRestMapper;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import com.processing.cardmanagement.services.CardGeneratorService;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardGeneratorController.class)
public class CardGeneratorControllerTest {

    private final Faker faker = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardRestMapper mapper;

    @MockitoBean
    private CardGeneratorService generatorService;


    @Test
    void generateShouldReturn201WhenValidRequest() throws Exception {
        List<String> bins = List.of("400000", "400001");
        GenerateCardsRequest request = new GenerateCardsRequest(2, bins);

        Card card1 = new Card(
            UUID.randomUUID(),
            faker.numerify("################"),
            "400000",
            faker.name().fullName().toUpperCase(),
            YearMonth.now().plusYears(3),
            CardStatus.ACTIVE,
            "643",
            BigDecimal.valueOf(15_000_000),
            BigDecimal.valueOf(300_000_000),
            BigDecimal.valueOf(100_000_000),
            "ZZZZZZ",
            LocalDateTime.now()
        );

        Card card2 = new Card(
            UUID.randomUUID(),
            faker.numerify("################"),
            "400001",
            faker.name().fullName().toUpperCase(),
            YearMonth.now().plusYears(3),
            CardStatus.ACTIVE,
            "643",
            BigDecimal.valueOf(15_000_000),
            BigDecimal.valueOf(300_000_000),
            BigDecimal.valueOf(100_000_000),
            "ZZZZZZ",
            LocalDateTime.now()
        );

        when(generatorService.generate(2, bins)).thenReturn(List.of(card1, card2));

        mockMvc.perform(post("/api/cards/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.generated").value(2));
    }

    @Test
    void generateShouldReturn400WhenCountIsZero() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(0, List.of("4000000"));

        mockMvc.perform(post("/api/cards/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generateShouldReturn400WhenInvalidBins() throws Exception {
        GenerateCardsRequest request = new GenerateCardsRequest(10, List.of("AAAAAA", "BBBBBB"));

        mockMvc.perform(post("/api/cards/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
