package com.stockbrokerage.kafka;

import com.stockbrokerage.events.PortfolioUpdateEvent;
import com.stockbrokerage.websocket.MarketDataWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioUpdateConsumer {
    
    private final MarketDataWebSocketHandler webSocketHandler;
    
    @KafkaListener(topics = "portfolio-updates", groupId = "websocket-notification-group")
    public void processPortfolioUpdate(PortfolioUpdateEvent event) {
        try {
            log.info("Processing portfolio update for user: {}", event.getUserId());
            
            // Create WebSocket message
            Map<String, Object> message = new HashMap<>();
            message.put("type", "PORTFOLIO_UPDATE");
            message.put("userId", event.getUserId());
            message.put("symbolId", event.getSymbolId());
            message.put("symbol", event.getSymbol());
            message.put("eventType", event.getEventType());
            message.put("quantityChange", event.getQuantityChange());
            message.put("balanceChange", event.getBalanceChange());
            message.put("timestamp", event.getTimestamp());
            
            // Send to user via WebSocket
            webSocketHandler.sendToUser(event.getUserId(), message);
            
            log.debug("Portfolio update sent to user: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing portfolio update", e);
        }
    }
}