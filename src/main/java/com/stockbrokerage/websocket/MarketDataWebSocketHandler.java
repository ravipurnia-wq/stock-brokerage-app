package com.stockbrokerage.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());
        
        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTION_ESTABLISHED\",\"message\":\"Connected to market data stream\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        sessionUserMap.remove(session.getId());
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
        
        // Handle subscription/unsubscription messages if needed
        // For now, all connected clients receive all market data
    }

    public void broadcastToAll(Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            broadcastMessage(jsonMessage);
        } catch (Exception e) {
            log.error("Error broadcasting message", e);
        }
    }
    
    public void sendToUser(String userId, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // Find sessions for the specific user
            sessionUserMap.entrySet().stream()
                .filter(entry -> userId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .forEach(sessionId -> {
                    WebSocketSession session = sessions.get(sessionId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(jsonMessage));
                        } catch (IOException e) {
                            log.error("Error sending message to user session {}", sessionId, e);
                        }
                    }
                });
        } catch (Exception e) {
            log.error("Error sending message to user", e);
        }
    }

    private void broadcastMessage(String message) {
        sessions.values().removeIf(session -> {
            if (!session.isOpen()) {
                return true; // Remove closed sessions
            }
            
            try {
                session.sendMessage(new TextMessage(message));
                return false; // Keep open sessions
            } catch (IOException e) {
                log.error("Error sending message to session {}", session.getId(), e);
                return true; // Remove sessions with send errors
            }
        });
    }

    public void registerUserSession(String sessionId, String userId) {
        sessionUserMap.put(sessionId, userId);
        log.debug("Registered user {} for session {}", userId, sessionId);
    }
}