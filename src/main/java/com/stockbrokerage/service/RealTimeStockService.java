package com.stockbrokerage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.events.MarketDataEvent;
import com.stockbrokerage.kafka.MarketDataProducer;
import com.stockbrokerage.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeStockService {
    
    private final SymbolRepository symbolRepository;
    private final MarketDataProducer marketDataProducer;
    private final RestTemplate restTemplate;
    private final FinnhubApiService finnhubApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Alpha Vantage API (free tier allows 5 calls per minute)
    private static final String ALPHA_VANTAGE_API_KEY = "demo"; // Use demo key for now
    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s";
    
    // Fallback to Yahoo Finance-style API (free)
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s";
    
    @Scheduled(fixedRate = 60000) // Every 60 seconds to avoid API limits
    public void fetchRealTimeStockPrices() {
        List<Symbol> symbols = symbolRepository.findByIsActiveTrue();
        
        for (Symbol symbol : symbols) {
            try {
                BigDecimal price = fetchStockPrice(symbol.getSymbol());
                if (price != null) {
                    publishMarketDataUpdate(symbol, price);
                    log.debug("Fetched real-time price for {}: ${}", symbol.getSymbol(), price);
                } else {
                    // Fallback to simulated data if API fails
                    BigDecimal simulatedPrice = generateSimulatedPrice(symbol.getSymbol());
                    publishMarketDataUpdate(symbol, simulatedPrice);
                    log.debug("Using simulated price for {}: ${}", symbol.getSymbol(), simulatedPrice);
                }
                
                // Small delay to avoid hitting API rate limits
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.warn("Error fetching price for {}: {}", symbol.getSymbol(), e.getMessage());
                // Use simulated data as fallback
                BigDecimal simulatedPrice = generateSimulatedPrice(symbol.getSymbol());
                publishMarketDataUpdate(symbol, simulatedPrice);
            }
        }
    }
    
    private BigDecimal fetchStockPrice(String symbolName) {
        try {
            // Try Finnhub API first (real-time data)
            FinnhubApiService.StockQuote quote = finnhubApiService.getStockQuote(symbolName);
            if (quote != null && quote.currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Fetched real-time price from Finnhub for {}: ${}", symbolName, quote.currentPrice);
                return quote.currentPrice;
            }
        } catch (Exception e) {
            log.debug("Finnhub API failed for {}: {}", symbolName, e.getMessage());
        }
        
        try {
            // Fallback to Yahoo Finance API
            String url = String.format(YAHOO_FINANCE_URL, symbolName);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode chart = root.path("chart").path("result").get(0);
                JsonNode meta = chart.path("meta");
                
                if (meta.has("regularMarketPrice")) {
                    double price = meta.path("regularMarketPrice").asDouble();
                    log.debug("Fetched price from Yahoo Finance for {}: ${}", symbolName, price);
                    return BigDecimal.valueOf(price).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (Exception e) {
            log.debug("Yahoo Finance API failed for {}: {}", symbolName, e.getMessage());
        }
        
        try {
            // Final fallback to Alpha Vantage API
            String url = String.format(ALPHA_VANTAGE_URL, symbolName, ALPHA_VANTAGE_API_KEY);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode quote = root.path("Global Quote");
                
                if (quote.has("05. price")) {
                    String priceStr = quote.path("05. price").asText();
                    log.debug("Fetched price from Alpha Vantage for {}: ${}", symbolName, priceStr);
                    return new BigDecimal(priceStr).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (Exception e) {
            log.debug("Alpha Vantage API failed for {}: {}", symbolName, e.getMessage());
        }
        
        return null; // All APIs failed, will use simulated data
    }
    
    private BigDecimal generateSimulatedPrice(String symbolName) {
        // Generate realistic prices based on symbol
        double basePrice;
        switch (symbolName) {
            case "AAPL": basePrice = 180.0; break;
            case "GOOGL": basePrice = 140.0; break;
            case "MSFT": basePrice = 350.0; break;
            case "TSLA": basePrice = 250.0; break;
            case "AMZN": basePrice = 130.0; break;
            default: basePrice = 100.0;
        }
        
        // Add random variation ±5%
        double variation = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
        double finalPrice = basePrice * (1 + variation);
        
        return BigDecimal.valueOf(finalPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private void publishMarketDataUpdate(Symbol symbol, BigDecimal price) {
        MarketDataEvent event = MarketDataEvent.builder()
                .symbolId(symbol.getId())
                .symbol(symbol.getSymbol())
                .price(price)
                .timestamp(LocalDateTime.now())
                .volume(ThreadLocalRandom.current().nextLong(1000, 50000))
                .build();
        
        marketDataProducer.publishMarketData(event);
    }
}