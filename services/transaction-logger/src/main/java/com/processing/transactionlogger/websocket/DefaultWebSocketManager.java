package com.processing.transactionlogger.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DefaultWebSocketManager implements WebSocketManager {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}, total: {}", session.getId(), sessions.size());
    }

    @Override
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {}, total: {}", session.getId(), sessions.size());
    }

    @Override
    public void broadcast(String message) {
        Set<WebSocketSession> deadSessions = new HashSet<>();
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                } else {
                    deadSessions.add(session);
                }
            } catch (Exception ex) {
                log.error("Failed to send message to session: {}", session.getId(), ex);
                deadSessions.add(session);
            }
        });
        sessions.removeAll(deadSessions);
    }

    @Scheduled(fixedDelayString = "${websocket.ping-interval-ms}")
    public void ping() {
        Set<WebSocketSession> deadSessions = new HashSet<>();
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new PingMessage());
                } else {
                    deadSessions.add(session);
                }
            } catch (Exception ex) {
                log.warn("Ping failed for session {}, removing", session.getId());
                deadSessions.add(session);
            }
        });
        sessions.removeAll(deadSessions);
    }
}
