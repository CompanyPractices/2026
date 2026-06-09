package com.processing.transactionlogger.service;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.specification.TransactionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransactionServiceIntegrationTest {
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void searchByPanReturnsOnlyMatchingTransactions() {
        save(transaction("4000000000000001", TransactionStatus.APPROVED));
        save(transaction("4000000000000002", TransactionStatus.APPROVED));

        TransactionFilter filter = new TransactionFilter();
        filter.setPan("4000000000000001");
        TransactionSearchResponse result = transactionService.search(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.transactions().get(0).pan()).isEqualTo("4000000000000001");
    }

    @Test
    void searchByStatusReturnsOnlyMatchingTransactions() {
        save(transaction("4000000000000001", TransactionStatus.APPROVED));
        save(transaction("4000000000000002", TransactionStatus.DECLINED));

        TransactionFilter filter = new TransactionFilter();
        filter.setStatus(TransactionStatus.APPROVED.toString());
        TransactionSearchResponse result = transactionService.search(filter);


        assertThat(result.total()).isEqualTo(1);
        assertThat(result.transactions().get(0).status()).isEqualTo(TransactionStatus.APPROVED);
    }

    @Test
    void searchByDateRangeReturnsTransactionsWithinRange() {
        Transaction old = transaction("4000000000000001", TransactionStatus.APPROVED);
        old.setTransmissionDateTime(Instant.now().minus(2, ChronoUnit.DAYS));
        save(old);
        save(transaction("4000000000000002", TransactionStatus.APPROVED));

        TransactionFilter filter = new TransactionFilter();
        filter.setDateFrom(LocalDate.now());
        TransactionSearchResponse result = transactionService.search(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.transactions().get(0).pan()).isEqualTo("4000000000000002");
    }

    @Test
    void searchReturnsTotalCountCorrectly() {
        save(transaction("4000000000000001", TransactionStatus.APPROVED));
        save(transaction("4000000000000002", TransactionStatus.APPROVED));
        save(transaction("4000000000000003", TransactionStatus.APPROVED));
        save(transaction("4000000000000004", TransactionStatus.APPROVED));

        TransactionFilter filter = new TransactionFilter();
        filter.setLimit(1);
        TransactionSearchResponse result = transactionService.search(filter);

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.transactions()).hasSize(1);
    }

    @Test
    void searchWithOffsetReturnsCorrectPage() {
        for (int i = 1; i <= 5; i++) {
            save(transaction("400000000000000" + i, TransactionStatus.APPROVED));
        }

        TransactionFilter filter = new TransactionFilter();
        filter.setLimit(2);
        filter.setOffset(2);
        TransactionSearchResponse result = transactionService.search(filter);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.transactions()).hasSize(2);
    }

    @Test
    void searchWithCombinedFiltersAppliesAndLogic() {
        save(transaction("4000000000000001", TransactionStatus.APPROVED));
        save(transaction("4000000000000001", TransactionStatus.DECLINED));
        save(transaction("4000000000000002", TransactionStatus.APPROVED));

        TransactionFilter filter = new TransactionFilter();
        filter.setPan("4000000000000001");
        filter.setStatus("APPROVED");
        TransactionSearchResponse result = transactionService.search(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.transactions().get(0).pan()).isEqualTo("4000000000000001");
        assertThat(result.transactions().get(0).status()).isEqualTo(TransactionStatus.APPROVED);
    }

    @Test
    void getRecentReturnsSortedByCreatedAtDesc() {
        Transaction first = transaction("4000000000000001", TransactionStatus.APPROVED);
        first.setCreatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        save(first);

        Transaction second = transaction("4000000000000002", TransactionStatus.APPROVED);
        second.setCreatedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        save(second);

        Transaction third = transaction("4000000000000003", TransactionStatus.APPROVED);
        third.setCreatedAt(Instant.now());
        save(third);

        List<TransactionResponse> result = transactionService.getRecent(10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).pan()).isEqualTo("4000000000000003");
        assertThat(result.get(2).pan()).isEqualTo("4000000000000001");
    }

    @Test
    void getRecentFollowsLimit() {
        for (int i = 1; i <= 5; i++) {
            save(transaction("400000000000000" + i, TransactionStatus.APPROVED));
        }

        List<TransactionResponse> result = transactionService.getRecent(3);

        assertThat(result).hasSize(3);
    }



    private Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    private Transaction transaction(String pan, TransactionStatus status) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setMti("0100");
        t.setStan("000001");
        t.setPan(pan);
        t.setProcessingCode("000000");
        t.setAmount(1000000L);
        t.setCurrencyCode("643");
        t.setTerminalId("TERM0001");
        t.setMerchantId("MERCHANT000001");
        t.setMcc("5411");
        t.setAcquirerId("ACQ001");
        t.setStatus(status);
        t.setTransmissionDateTime(Instant.now());
        t.setCreatedAt(Instant.now());
        return t;
    }
}
