package com.stockbrokerage.service;

import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.events.MarketDataEvent;
import com.stockbrokerage.kafka.MarketDataProducer;
import com.stockbrokerage.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {
    
    private final SymbolRepository symbolRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MarketDataProducer marketDataProducer;
    private final Random random = new Random();
    
    public BigDecimal getCurrentPrice(String symbolId) {
        String key = "market:price:" + symbolId;
        Object price = redisTemplate.opsForValue().get(key);
        
        if (price instanceof BigDecimal) {
            return (BigDecimal) price;
        } else if (price instanceof String) {
            return new BigDecimal((String) price);
        } else if (price instanceof Number) {
            return BigDecimal.valueOf(((Number) price).doubleValue());
        }
        
        // Return a default price if not found in cache
        return BigDecimal.valueOf(100.00);
    }
    
    public void updatePrice(String symbolId, BigDecimal price) {
        String key = "market:price:" + symbolId;
        redisTemplate.opsForValue().set(key, price, Duration.ofHours(1));
        
        // Publish price update to Kafka
        Symbol symbol = symbolRepository.findById(symbolId).orElse(null);
        if (symbol != null) {
            MarketDataEvent event = MarketDataEvent.builder()
                    .symbolId(symbolId)
                    .symbol(symbol.getSymbol())
                    .price(price)
                    .timestamp(LocalDateTime.now())
                    .volume(ThreadLocalRandom.current().nextLong(1000, 10000))
                    .build();
            
            marketDataProducer.publishMarketData(event);
        }
    }
    
    @KafkaListener(topics = "market-data", groupId = "stock-brokerage-group")
    public void handleMarketDataUpdate(MarketDataEvent event) {
        log.info("Received market data update: {} - {}", event.getSymbol(), event.getPrice());
        
        // Update cache
        String key = "market:price:" + event.getSymbolId();
        redisTemplate.opsForValue().set(key, event.getPrice(), Duration.ofHours(1));
        
        // You can add additional processing here like:
        // - Updating database with historical data
        // - Triggering stop-loss orders
        // - Sending notifications to users
    }
    
    // @Scheduled(fixedRate = 30000) // Disabled - using RealTimeStockService instead
    public void simulateMarketData() {
        List<Symbol> symbols = symbolRepository.findByIsActiveTrue();
        
        for (Symbol symbol : symbols) {
            // Simulate price movement
            BigDecimal currentPrice = getCurrentPrice(symbol.getId());
            
            // Random price change between -5% to +5%
            double changePercent = (random.nextDouble() - 0.5) * 0.1; // -5% to +5%
            BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Ensure price doesn't go below $1
            if (newPrice.compareTo(BigDecimal.ONE) < 0) {
                newPrice = BigDecimal.ONE;
            }
            
            updatePrice(symbol.getId(), newPrice);
        }
    }
    
    public MarketDataEvent getMarketData(String symbolId) {
        Symbol symbol = symbolRepository.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        BigDecimal currentPrice = getCurrentPrice(symbolId);
        
        return MarketDataEvent.builder()
                .symbolId(symbolId)
                .symbol(symbol.getSymbol())
                .price(currentPrice)
                .timestamp(LocalDateTime.now())
                .volume(ThreadLocalRandom.current().nextLong(1000, 10000))
                .build();
    }
    
    public List<Symbol> getAllActiveSymbols() {
        return symbolRepository.findByIsActiveTrue();
    }
    
    public void initializeMarketData() {
        // Initialize some default symbols and prices
        List<Symbol> symbols = List.of(
                Symbol.builder().symbol("AAPL").companyName("Apple Inc.").exchange("NASDAQ").sector("Technology").isActive(true).build(),
                Symbol.builder().symbol("GOOGL").companyName("Alphabet Inc.").exchange("NASDAQ").sector("Technology").isActive(true).build(),
                Symbol.builder().symbol("MSFT").companyName("Microsoft Corporation").exchange("NASDAQ").sector("Technology").isActive(true).build(),
                Symbol.builder().symbol("TSLA").companyName("Tesla Inc.").exchange("NASDAQ").sector("Automotive").isActive(true).build(),
                Symbol.builder().symbol("AMZN").companyName("Amazon.com Inc.").exchange("NASDAQ").sector("E-commerce").isActive(true).build()
        );
        
        for (Symbol symbol : symbols) {
            Symbol existingSymbol = symbolRepository.findBySymbol(symbol.getSymbol()).orElse(null);
            if (existingSymbol == null) {
                Symbol savedSymbol = symbolRepository.save(symbol);
                
                // Initialize with random price between $50-$300
                BigDecimal initialPrice = BigDecimal.valueOf(50 + random.nextDouble() * 250)
                        .setScale(2, RoundingMode.HALF_UP);
                updatePrice(savedSymbol.getId(), initialPrice);
                
                log.info("Initialized symbol: {} with price: {}", symbol.getSymbol(), initialPrice);
            }
        }
    }
}