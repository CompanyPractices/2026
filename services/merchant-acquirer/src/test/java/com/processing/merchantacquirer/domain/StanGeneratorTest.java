package com.processing.merchantacquirer.domain;

import com.processing.merchantacquirer.domain.service.StanGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StanGeneratorTest {
    @Test
    void firstStanIsOne() {
        assertEquals("000001", new StanGenerator().next("TERM0001"));
    }

    @Test
    void succesfullIncrementStan() {
        StanGenerator gen = new StanGenerator();
        assertEquals("000001", gen.next("TERM0001"));
        assertEquals("000002", gen.next("TERM0001"));
        assertEquals("000003", gen.next("TERM0001"));
    }

    @Test
    void differentTerminalsReturnsDifferentsStans() {
        StanGenerator gen = new StanGenerator();
        assertEquals("000001", gen.next("TERM0001"));
        assertEquals("000001", gen.next("TERM0002"));
        assertEquals("000002", gen.next("TERM0001"));
        assertEquals("000002", gen.next("TERM0002"));
        assertEquals("000003", gen.next("TERM0001"));
        assertEquals("000003", gen.next("TERM0002"));
    }

    @Test
    void overStan() throws NoSuchFieldException, IllegalAccessException {
        StanGenerator gen = new StanGenerator();
        Field f = StanGenerator.class.getDeclaredField("counters");
        f.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, AtomicInteger> counters= (Map<String, AtomicInteger>) f.get(gen);
        counters.put("TERM0001", new AtomicInteger(999999));

        assertEquals("000001", gen.next("TERM0001"));

    }

    @Test
    void noDuplicatesUnderConcurrency() {
        StanGenerator gen = new StanGenerator();
        int threads = 8;
        int perThread = 2000;
        Set<String> produced = ConcurrentHashMap.newKeySet();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++){
                futures.add(pool.submit(() -> {
                    for (int j = 0; j < perThread; j++){

                    produced.add(gen.next("TERM0001"));
                        }
                }));
            }
            for (Future<?> future : futures){
                future.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(threads*perThread, produced.size());
        assertTrue(produced.contains("000001"));
    }
}
