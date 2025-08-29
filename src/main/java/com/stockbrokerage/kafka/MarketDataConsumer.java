package com.stockbrokerage.kafka;

import com.stockbrokerage.events.MarketDataEvent;
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
public class MarketDataConsumer {
    
    private final MarketDataWebSocketHandler webSocketHandler;
    
    @KafkaListener(topics = "market-data", groupId = "websocket-broadcast-group")
    public void processMarketDataUpdate(MarketDataEvent event) {
        try {
            log.debug("Broadcasting market data update: {} - {}", event.getSymbol(), event.getPrice());
            
            // Create WebSocket message
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MARKET_DATA_UPDATE");
            message.put("symbolId", event.getSymbolId());
            message.put("symbol", event.getSymbol());
            message.put("price", event.getPrice());
            message.put("volume", event.getVolume());
            message.put("timestamp", event.getTimestamp());
            
            // Broadcast to all connected clients
            webSocketHandler.broadcastToAll(message);
            
        } catch (Exception e) {
            log.error("Error broadcasting market data update", e);
        }
    }
}