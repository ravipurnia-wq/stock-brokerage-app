package com.stockbrokerage.kafka;

import com.stockbrokerage.events.MarketDataEvent;
import com.stockbrokerage.events.OrderPlacedEvent;
import com.stockbrokerage.events.PortfolioUpdateEvent;
import com.stockbrokerage.events.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        publishEvent("order-events", event.getOrderId(), event, "Order placed event");
    }
    
    public void publishTradeExecutedEvent(TradeExecutedEvent event) {
        publishEvent("trade-events", event.getTradeId(), event, "Trade executed event");
    }
    
    public void publishMarketDataEvent(MarketDataEvent event) {
        publishEvent("market-data", event.getSymbolId(), event, "Market data event");
    }
    
    public void publishPortfolioUpdateEvent(PortfolioUpdateEvent event) {
        publishEvent("portfolio-updates", event.getUserId(), event, "Portfolio update event");
    }
    
    private void publishEvent(String topic, String key, Object event, String eventDescription) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, event);
                
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("{} published successfully to topic: {} with key: {}", 
                        eventDescription, topic, key);
                } else {
                    log.error("Failed to publish {} to topic: {} with key: {}", 
                        eventDescription, topic, key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing {} to topic: {}", eventDescription, topic, e);
        }
    }
}