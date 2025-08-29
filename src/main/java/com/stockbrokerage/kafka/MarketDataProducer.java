package com.stockbrokerage.kafka;

import com.stockbrokerage.events.MarketDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishMarketData(MarketDataEvent marketDataEvent) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("market-data", marketDataEvent.getSymbolId(), marketDataEvent);
                
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Market data published successfully for symbol: {} at price: {}", 
                        marketDataEvent.getSymbol(), marketDataEvent.getPrice());
                } else {
                    log.error("Failed to publish market data for symbol: {}", 
                        marketDataEvent.getSymbol(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing market data event", e);
        }
    }
}