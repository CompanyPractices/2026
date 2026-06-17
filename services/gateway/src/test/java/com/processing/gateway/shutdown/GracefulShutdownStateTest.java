package com.processing.gateway.shutdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GracefulShutdownStateTest {

    @Test
    void startsInAcceptingRequestsState() {
        GracefulShutdownState shutdownState = new GracefulShutdownState();

        assertThat(shutdownState.isShuttingDown()).isFalse();
    }

    @Test
    void switchesToShuttingDownState() {
        GracefulShutdownState shutdownState = new GracefulShutdownState();

        shutdownState.startShutdown();

        assertThat(shutdownState.isShuttingDown()).isTrue();
    }
}
