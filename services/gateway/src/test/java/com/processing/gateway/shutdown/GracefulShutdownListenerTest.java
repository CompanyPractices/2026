package com.processing.gateway.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class GracefulShutdownListenerTest {
    @Test
    void startsShutdownWhenApplicationContextCloses() {
        GracefulShutdownState shutdownState = new GracefulShutdownState();
        GracefulShutdownListener listener = new GracefulShutdownListener(shutdownState);
        StaticApplicationContext context = new StaticApplicationContext();

        listener.onApplicationEvent(new ContextClosedEvent(context));

        assertThat(shutdownState.isShuttingDown()).isTrue();
    }
}
