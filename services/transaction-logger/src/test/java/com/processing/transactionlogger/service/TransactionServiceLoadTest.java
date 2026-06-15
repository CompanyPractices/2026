package com.processing.transactionlogger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionServiceLoadTest {
    private static final int THREADS = 20;
    private static final int STORE_REQUESTS_PER_THREAD = 5;
    private static final int SEARCH_DATASET_SIZE = 100;
    private static final int SEARCH_THREADS = 10;
    private static final int SEARCH_REQUESTS_PER_THREAD = 10;
    private static final long SEARCH_P99_LIMIT_MS = 500;
    private static final long STATS_P99_LIMIT_MS = 1000;
    private static final long RECENT_P99_LIMIT_MS = 500;
    private static final long STORE_P99_LIMIT_MS = 2000;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAllInBatch();
    }

    @Test
    void storeConcurrentlyWithUniqueIdsYieldsNoErrors() throws Exception {
        int requestsPerThread = 5;
        AtomicInteger created = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(THREADS, () -> {
            for (int i = 0; i < requestsPerThread; i++) {
                int status = storeTransaction(transactionRequest(UUID.randomUUID()));
                if (status == 201) {
                    created.incrementAndGet();
                } else {
                    errors.incrementAndGet();
                }
            }
        });

        assertEquals(0, errors.get());
        assertEquals(THREADS * requestsPerThread, created.get());
        assertEquals(THREADS * requestsPerThread, transactionRepository.count());
    }

    @Test
    void storeIdempotentUnderConcurrency() throws Exception {
        UUID id = UUID.randomUUID();

        AtomicInteger created = new AtomicInteger();
        AtomicInteger returned = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        runConcurrently(THREADS, () -> {
            int status = storeTransaction(transactionRequest(id));
            switch (status) {
                case 201 -> created.incrementAndGet();
                case 200 -> returned.incrementAndGet();
                case 409 -> conflicts.incrementAndGet();
            }
        });

        assertEquals(0, conflicts.get());
        assertEquals(1, created.get());
        assertEquals(THREADS - 1, returned.get());
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void searchWhileStoringConcurrently() throws Exception {
        int total = THREADS + SEARCH_THREADS;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger storeErrors = new AtomicInteger();
        AtomicInteger searchErrors = new AtomicInteger();

        try (ExecutorService executorService = Executors.newFixedThreadPool(total)) {
            for (int i = 0; i < THREADS; i++) {
                executorService.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < STORE_REQUESTS_PER_THREAD; j++) {
                            int status = storeTransaction(transactionRequest(UUID.randomUUID()));
                            if (status != 201) {
                                storeErrors.incrementAndGet();
                            }
                        }
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            for (int i = 0; i < SEARCH_THREADS; i++) {
                executorService.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < SEARCH_REQUESTS_PER_THREAD; j++) {
                            int status = searchTransactions();
                            if (status != 200) {
                                searchErrors.incrementAndGet();
                            }
                        }
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS));
        }

        assertThat(failures).isEmpty();
        assertEquals(0, storeErrors.get());
        assertEquals(0, searchErrors.get());
        assertEquals(THREADS * STORE_REQUESTS_PER_THREAD, transactionRepository.count());
    }

    @Test
    void storeConcurrencyMeetsLatencySla() throws Exception {
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(THREADS, () -> {
            for (int i = 0; i < STORE_REQUESTS_PER_THREAD; i++) {
                long start = System.nanoTime();
                int status = storeTransaction(transactionRequest(UUID.randomUUID()));
                long ms = (System.nanoTime() - start) / 1_000_000;
                if (status == 201) {
                    latenciesMs.add(ms);
                } else {
                    errors.incrementAndGet();
                }
            }
        });

        assertEquals(0, errors.get());
        assertThat(percentile(latenciesMs, 99)).isLessThanOrEqualTo(STORE_P99_LIMIT_MS);
    }

    @Test
    void searchConcurrentlyMeetsLatencySla() throws Exception {
        for (int i = 0; i < SEARCH_DATASET_SIZE; i++) {
            storeTransaction(transactionRequest(UUID.randomUUID()));
        }

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(SEARCH_THREADS, () -> {
            for (int i = 0; i < SEARCH_REQUESTS_PER_THREAD; i++) {
                long start = System.nanoTime();
                int status = searchTransactions();
                long ms = (System.nanoTime() - start) / 1_000_000;
                if (status == 200) {
                    latenciesMs.add(ms);
                } else {
                    errors.incrementAndGet();
                }
            }
        });

        assertEquals(0, errors.get());
        assertThat(percentile(latenciesMs, 99)).isLessThanOrEqualTo(SEARCH_P99_LIMIT_MS);
    }

    @Test
    void getStatsConcurrentlyMeetsLatencySla() throws Exception {
        for (int i = 0; i < SEARCH_DATASET_SIZE; i++) {
            storeTransaction(transactionRequest(UUID.randomUUID()));
        }

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(SEARCH_THREADS, () -> {
            for (int i = 0; i < SEARCH_REQUESTS_PER_THREAD; i++) {
                long start = System.nanoTime();
                int status = getStats();
                long ms = (System.nanoTime() - start) / 1_000_000;
                if (status == 200) {
                    latenciesMs.add(ms);
                } else {
                    errors.incrementAndGet();
                }
            }
        });

        assertEquals(0, errors.get());
        assertThat(percentile(latenciesMs, 99)).isLessThanOrEqualTo(STATS_P99_LIMIT_MS);
    }

    @Test
    void getRecentConcurrentlyMeetsLatencySla() throws Exception {
        for (int i = 0; i < SEARCH_DATASET_SIZE; i++) {
            storeTransaction(transactionRequest(UUID.randomUUID()));
        }

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(SEARCH_THREADS, () -> {
            for (int i = 0; i < SEARCH_REQUESTS_PER_THREAD; i++) {
                long start = System.nanoTime();
                int status = getRecent();
                long ms = (System.nanoTime() - start) / 1_000_000;
                if (status == 200) {
                    latenciesMs.add(ms);
                } else {
                    errors.incrementAndGet();
                }
            }
        });

        assertEquals(0, errors.get());
        assertThat(percentile(latenciesMs, 99)).isLessThanOrEqualTo(RECENT_P99_LIMIT_MS);
    }

    private int getStats() throws Exception {
        return mockMvc.perform(get("/api/dashboard/stats"))
                .andReturn().getResponse().getStatus();
    }

    private int getRecent() throws Exception {
        return mockMvc.perform(get("/api/dashboard/recent"))
                .andReturn().getResponse().getStatus();
    }

    private int storeTransaction(TransactionRequest request) throws Exception {
        return mockMvc.perform(post("/api/internal/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getStatus();
    }

    private int searchTransactions() throws Exception {
        return mockMvc.perform(get("/api/transactions/search"))
                .andReturn().getResponse().getStatus();
    }

    private void runConcurrently(int threads, ThrowingRunnable task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                executorService.submit(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS));
        }
        assertThat(failures).isEmpty();
    }

    private static long percentile(List<Long> values, int p) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) (Math.ceil(p / 100.0 * sorted.size()) - 1);
        return sorted.get(Math.max(0, index));
    }

    private static TransactionRequest transactionRequest(UUID id) {
        return new TransactionRequest(
                id,
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                150000L,
                "643",
                "TERM001",
                "POS",
                "MERCH1234567890",
                "5411",
                "ACQ001",
                "ISS001",
                2250L,
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
