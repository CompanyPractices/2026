package com.processing.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface WebSocketManager {
    void addSession(WebSocketSession session);
    void removeSession(WebSocketSession session);
    void broadcast(String message);
}
