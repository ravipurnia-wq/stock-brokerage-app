package com.stockbrokerage.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockbrokerage.events.MarketDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
        
        // Handle subscription/unsubscription messages if needed
        // For now, all connected clients receive all market data
    }

    @KafkaListener(topics = "market-data", groupId = "websocket-group")
    public void handleMarketDataUpdate(MarketDataEvent event) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                "type", "MARKET_DATA_UPDATE",
                "data", event
            ));
            
            broadcastMessage(message);
            log.debug("Broadcasted market data update: {} - {}", event.getSymbol(), event.getPrice());
            
        } catch (Exception e) {
            log.error("Error broadcasting market data update", e);
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

    public void sendPortfolioUpdate(String userId, Object portfolioData) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                "type", "PORTFOLIO_UPDATE",
                "userId", userId,
                "data", portfolioData
            ));
            
            // In a real implementation, you would filter by user session
            // For now, we'll broadcast to all sessions
            broadcastMessage(message);
            
        } catch (Exception e) {
            log.error("Error sending portfolio update", e);
        }
    }

    public void sendOrderUpdate(String userId, Object orderData) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                "type", "ORDER_UPDATE",
                "userId", userId,
                "data", orderData
            ));
            
            // In a real implementation, you would filter by user session
            broadcastMessage(message);
            
        } catch (Exception e) {
            log.error("Error sending order update", e);
        }
    }
}