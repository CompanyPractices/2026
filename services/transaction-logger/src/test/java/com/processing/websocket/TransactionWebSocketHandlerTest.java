package com.processing.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TransactionWebSocketHandlerTest {
    @Mock
    private WebSocketManager webSocketManager;
    @Mock
    private WebSocketSession session;
    private TransactionWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionWebSocketHandler(webSocketManager);
    }

    @Test
    void afterConnectionEstablishedAddsSessionToManager() {
        handler.afterConnectionEstablished(session);

        verify(webSocketManager).addSession(session);
    }

    @Test
    void afterConnectionClosedRemovesSessionFromManager() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(webSocketManager).removeSession(session);
    }
}
