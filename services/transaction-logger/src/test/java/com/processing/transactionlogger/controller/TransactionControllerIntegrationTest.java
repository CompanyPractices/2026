package com.processing.controller;

import com.processing.dto.TransactionSearchResponse;
import com.processing.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
public class TransactionControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void searchReturns200withValidParameters() throws Exception {
        when(transactionService.search(any())).thenReturn(new TransactionSearchResponse(5L, List.of()));

        mockMvc.perform(get("/api/transactions/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.transactions").isArray());
    }

    @Test
    void searchReturns400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/transactions/search").param("status","UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturns400WhenDateFromIsAfterDateTo() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                .param("dateFrom", "2026-06-08")
                .param("dateTo", "2026-06-01"))
                .andExpect(status().isBadRequest());
    }

}
