package com.processing.cardmanagement.load;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@RequiredArgsConstructor
public class LoadTestEngine {

    private final int maximumParallelRequests;
    private final int maximumTotalRequests;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<Void> run(Runnable runnable) {
        Semaphore semaphore = new Semaphore(maximumParallelRequests);
        List<CompletableFuture<Void>> futures = new ArrayList<>(maximumTotalRequests);

        for (int i = 0; i < maximumTotalRequests; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    runnable.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Task interrupted", e);
                } finally {
                    semaphore.release();
                }
            }, executor);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
}
