package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        mockMvc.perform(get("/api/transactions/search").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturns400WhenDateFromIsAfterDateTo() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("dateFrom", "2026-06-08")
                        .param("dateTo", "2026-06-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportReturns200WithCsvFileAttachment() throws Exception {
        when(transactionService.exportCsv(any())).thenReturn("id,mti\r\n0001,0100\r\n");

        mockMvc.perform(get("/api/transactions/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("transactions.csv")))
                .andExpect(content().string(startsWith("id,mti")));
    }

    @Test
    void exportReturns400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/transactions/export").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
