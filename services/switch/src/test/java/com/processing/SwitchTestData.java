package com.processing;

import com.processing.config.SwitchProperties;
import com.processing.model.AuthorizationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SwitchTestData {

    public static final Map<String, String> BIN_ROUTING = Map.of(
            "400000", "ISS001",
            "400001", "ISS002",
            "400002", "ISS003",
            "400003", "ISS004",
            "400004", "ISS005"
    );

    private SwitchTestData() {
    }

    public static SwitchProperties.HttpProperties defaultHttp() {
        return new SwitchProperties.HttpProperties(3000, 5000, 2000);
    }

    public static SwitchProperties.RetryProperties defaultRetry() {
        return new SwitchProperties.RetryProperties(3, List.of(0L, 0L, 0L));
    }

    public static SwitchProperties defaultProperties() {
        return new SwitchProperties(
                "1.0.0",
                BIN_ROUTING,
                "http://localhost:8083",
                "http://localhost:8088",
                defaultHttp(),
                defaultRetry()
        );
    }

    public static AuthorizationRequest sampleRequest() {
        return new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560001",
                "000000",
                150_000L,
                "643",
                LocalDateTime.parse("2026-06-01T10:30:00"),
                "TERM001",
                null,
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null,
                null
        );
    }
}
