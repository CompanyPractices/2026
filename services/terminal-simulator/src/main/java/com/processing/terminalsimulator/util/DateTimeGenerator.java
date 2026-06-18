package com.processing.terminalsimulator.util;


import com.processing.terminalsimulator.model.PartofDay;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DateTimeGenerator {
    public Instant generate(PartofDay partOfDay) {
        int year = 2026;
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, 28);

        int hour;
        if (PartofDay.NIGHT.equals(partOfDay)) {
            hour = ThreadLocalRandom.current().nextInt(1, 5);
        } else {
            hour = ThreadLocalRandom.current().nextInt(9, 22);
        }
        int minute = ThreadLocalRandom.current().nextInt(0, 60);
        int second = ThreadLocalRandom.current().nextInt(0, 60);

        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second);
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
