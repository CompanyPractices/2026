package com.processing.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DefaultWebSocketManager implements WebSocketManager{
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}, total: {}", session.getId(), sessions.size());
    }

    @Override
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {}, total: {}, session.getId()", session.getId(), sessions.size());
    }

    @Override
    public void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception ex) {
                log.error("Failed to send message to session: {}", session.getId(), ex);
                sessions.remove(session);
            }
        });
    }
}
