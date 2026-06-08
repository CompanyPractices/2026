package com.processing.cardmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.services.CardGeneratorService;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(CardGeneratorController.class)
public class CardGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardGeneratorService generatorService;

    @Test
    void generateShouldReturn201WhenValidRequest() throws Exception {
        List<String> bins = List.of("400000", "401000");
        GenerateCardsRequest request = new GenerateCardsRequest(2, bins);

        CardModel card1 = new CardModel(
                UUID.randomUUID(),
                "4000001234567893",
                "400000",
                "IVAN IVANOV",
                "0629",
                "ACTIVE",
                "643",
                15_000_000L,
                300_000_000L,
                100_000_000,
                "ZZZZZZ",
                LocalDateTime.now()
        );

        CardModel card2 = new CardModel(
                UUID.randomUUID(),
                "4000001234567893",
                "400001",
                "IVAN IVANOV",
                "0629",
                "ACTIVE",
                "643",
                15_000_000L,
                300_000_000L,
                100_000_000,
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
