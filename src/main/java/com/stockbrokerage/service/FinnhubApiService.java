package com.stockbrokerage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubApiService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${finnhub.api.base-url}")
    private String baseUrl;
    
    @Value("${finnhub.api.token}")
    private String apiToken;
    
    @Value("${finnhub.api.rate-limit:30}")
    private int rateLimit;
    
    // Rate limiting
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    private final long REQUEST_INTERVAL_MS = 1000 / 30; // 30 requests per second
    
    // Cache to store recent prices
    private final ConcurrentHashMap<String, StockQuote> priceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long CACHE_DURATION_MS = 5000; // 5 seconds cache
    
    public static class StockQuote {
        public final BigDecimal currentPrice;
        public final BigDecimal change;
        public final BigDecimal changePercent;
        public final BigDecimal highPrice;
        public final BigDecimal lowPrice;
        public final BigDecimal openPrice;
        public final BigDecimal previousClose;
        public final long timestamp;
        
        public StockQuote(BigDecimal currentPrice, BigDecimal change, BigDecimal changePercent,
                         BigDecimal highPrice, BigDecimal lowPrice, BigDecimal openPrice,
                         BigDecimal previousClose) {
            this.currentPrice = currentPrice;
            this.change = change;
            this.changePercent = changePercent;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.openPrice = openPrice;
            this.previousClose = previousClose;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Get real-time stock quote from Finnhub API
     */
    public StockQuote getStockQuote(String symbol) {
        try {
            // Check cache first
            if (isCacheValid(symbol)) {
                log.debug("Returning cached price for {}", symbol);
                return priceCache.get(symbol);
            }
            
            // Rate limiting
            enforceRateLimit();
            
            String url = String.format("%s/quote?symbol=%s&token=%s", baseUrl, symbol.toUpperCase(), apiToken);
            
            log.debug("Fetching real-time price for {} from Finnhub", symbol);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // Check if the response contains valid data
                if (jsonNode.has("c") && jsonNode.get("c").asDouble() > 0) {
                    StockQuote quote = parseQuoteResponse(jsonNode);
                    
                    // Cache the result
                    priceCache.put(symbol, quote);
                    cacheTimestamps.put(symbol, System.currentTimeMillis());
                    
                    log.info("Retrieved real-time price for {}: ${}", symbol, quote.currentPrice);
                    return quote;
                } else {
                    log.warn("Invalid or empty response for symbol {}: {}", symbol, response);
                }
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded for Finnhub API. Symbol: {}", symbol);
            } else {
                log.error("HTTP error fetching price for {}: {}", symbol, e.getMessage());
            }
        } catch (ResourceAccessException e) {
            log.error("Network error fetching price for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching price for {} from Finnhub: {}", symbol, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get just the current price for a symbol
     */
    public BigDecimal getCurrentPrice(String symbol) {
        StockQuote quote = getStockQuote(symbol);
        return quote != null ? quote.currentPrice : null;
    }
    
    /**
     * Check if multiple symbols are supported (batch operation)
     */
    public boolean isSymbolSupported(String symbol) {
        try {
            StockQuote quote = getStockQuote(symbol);
            return quote != null && quote.currentPrice.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            log.debug("Symbol {} not supported or error occurred: {}", symbol, e.getMessage());
            return false;
        }
    }
    
    private StockQuote parseQuoteResponse(JsonNode jsonNode) {
        BigDecimal currentPrice = BigDecimal.valueOf(jsonNode.get("c").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = BigDecimal.valueOf(jsonNode.get("d").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = BigDecimal.valueOf(jsonNode.get("dp").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal highPrice = BigDecimal.valueOf(jsonNode.get("h").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal lowPrice = BigDecimal.valueOf(jsonNode.get("l").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal openPrice = BigDecimal.valueOf(jsonNode.get("o").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousClose = BigDecimal.valueOf(jsonNode.get("pc").asDouble())
                .setScale(2, RoundingMode.HALF_UP);
        
        return new StockQuote(currentPrice, change, changePercent, highPrice, 
                            lowPrice, openPrice, previousClose);
    }
    
    private boolean isCacheValid(String symbol) {
        Long cacheTime = cacheTimestamps.get(symbol);
        if (cacheTime == null) {
            return false;
        }
        
        return (System.currentTimeMillis() - cacheTime) < CACHE_DURATION_MS;
    }
    
    private void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long lastRequest = lastRequestTime.get();
        long timeSinceLastRequest = now - lastRequest;
        
        if (timeSinceLastRequest < REQUEST_INTERVAL_MS) {
            try {
                long sleepTime = REQUEST_INTERVAL_MS - timeSinceLastRequest;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate limiting sleep interrupted");
            }
        }
        
        lastRequestTime.set(System.currentTimeMillis());
    }
    
    /**
     * Clear cache for testing or manual refresh
     */
    public void clearCache() {
        priceCache.clear();
        cacheTimestamps.clear();
        log.info("Finnhub price cache cleared");
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("Cache size: %d symbols, Rate limit: %d calls/sec", 
                           priceCache.size(), rateLimit);
    }
}