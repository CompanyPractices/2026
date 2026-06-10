package com.processing.transactionlogger.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultWebSocketManagerTest {
    @Mock
    private WebSocketSession session;
    private DefaultWebSocketManager manager;
    private static final String TEST_MESSAGE = "TEST_MESSAGE";

    @BeforeEach
    void setUp() {
        manager = new DefaultWebSocketManager();
    }

    @Test
    void addSessionRegistersSession() throws Exception {
        when(session.isOpen()).thenReturn(true);
        manager.addSession(session);

        manager.broadcast(TEST_MESSAGE);

        verify(session).sendMessage(new TextMessage(TEST_MESSAGE));
    }

    @Test
    void removeSessionUnregistersSession() throws Exception {
        manager.addSession(session);

        manager.removeSession(session);
        manager.broadcast(TEST_MESSAGE);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void broadcastSendsMessageToAllOpenSessions() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(true);
        manager.addSession(session1);
        manager.addSession(session2);
        manager.addSession(session3);

        manager.broadcast(TEST_MESSAGE);

        verify(session1).sendMessage(new TextMessage(TEST_MESSAGE));
        verify(session1).sendMessage(new TextMessage(TEST_MESSAGE));
        verify(session1).sendMessage(new TextMessage(TEST_MESSAGE));
    }

    @Test
    void broadcastSkipsClosedSessions() throws Exception {
        when(session.isOpen()).thenReturn(false);
        manager.addSession(session);

        manager.broadcast(TEST_MESSAGE);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void broadcastDoesNotThrowWhenSessionFails() throws IOException {
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("SENDING_ERROR")).when(session).sendMessage(any());
        manager.addSession(session);

        assertThatNoException().isThrownBy(() -> manager.broadcast(TEST_MESSAGE));
    }

    @Test
    void broadcastContinuesToOtherSessionsWhenOneFails() throws Exception {
        DefaultWebSocketManager manager = new DefaultWebSocketManager();
        WebSocketSession failing = mock(WebSocketSession.class);
        WebSocketSession healthy = mock(WebSocketSession.class);
        when(failing.isOpen()).thenReturn(false);
        when(healthy.isOpen()).thenReturn(true);
        lenient().doThrow(new IOException("SENDING_ERROR")).when(failing).sendMessage(any());
        manager.addSession(failing);
        manager.addSession(healthy);

        manager.broadcast(TEST_MESSAGE);

        verify(healthy).sendMessage(new TextMessage(TEST_MESSAGE));
    }

    @Test
    void pingSendsMessageToOpenSessions() throws Exception {
        when(session.isOpen()).thenReturn(true);
        manager.addSession(session);

        manager.ping();

        verify(session).sendMessage(any(PingMessage.class));
    }

    @Test
    void pingRemovesClosedSession() throws Exception {
        when(session.isOpen()).thenReturn(false);
        manager.addSession(session);

        manager.ping();
        manager.broadcast(TEST_MESSAGE);

        verify(session, never()).sendMessage(any());
    }
    @Test
    void pingRemovesSessionThatFailsToReceivePing() throws Exception {
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("PING_ERROR")).when(session).sendMessage(any(PingMessage.class));
        manager.addSession(session);

        manager.ping();
        manager.broadcast(TEST_MESSAGE);

        verify(session, times(1)).sendMessage(any());
    }
}
