package com.processing.transactionlogger.controller;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.service.TransactionService;
import com.processing.transactionlogger.specification.TransactionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {
    @Mock
    private TransactionService transactionService;
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        transactionController = new TransactionController(transactionService);
    }

    @Test
    void searchReturnsResultFromService() {
        TransactionSearchResponse expected = new TransactionSearchResponse(10L, List.of());
        when(transactionService.search(any())).thenReturn(expected);

        TransactionSearchResponse result = transactionController.search(new TransactionFilter());

        assertEquals(expected, result);
    }

    @Test
    void searchPassesFilterToService() {
        when(transactionService.search(any())).thenReturn(new TransactionSearchResponse(0L, List.of()));
        TransactionFilter filter = new TransactionFilter();
        filter.setPan("4000001234560001");
        filter.setStatus(TransactionStatus.APPROVED);

        transactionController.search(filter);

        verify(transactionService).search(filter);
    }
}
