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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    
    public static class HistoricalData {
        public final List<CandleData> candles;
        public final String symbol;
        public final String resolution;
        public final long fromTimestamp;
        public final long toTimestamp;
        
        public HistoricalData(List<CandleData> candles, String symbol, String resolution, 
                            long fromTimestamp, long toTimestamp) {
            this.candles = candles;
            this.symbol = symbol;
            this.resolution = resolution;
            this.fromTimestamp = fromTimestamp;
            this.toTimestamp = toTimestamp;
        }
    }
    
    public static class CandleData {
        public final BigDecimal open;
        public final BigDecimal high;
        public final BigDecimal low;
        public final BigDecimal close;
        public final long volume;
        public final long timestamp;
        
        public CandleData(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                         long volume, long timestamp) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.timestamp = timestamp;
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
     * Get historical stock data for charts
     */
    public HistoricalData getHistoricalData(String symbol, String resolution, long fromTimestamp, long toTimestamp) {
        try {
            // Rate limiting
            enforceRateLimit();
            
            String url = String.format("%s/stock/candle?symbol=%s&resolution=%s&from=%d&to=%d&token=%s", 
                    baseUrl, symbol.toUpperCase(), resolution, fromTimestamp, toTimestamp, apiToken);
            
            log.debug("Fetching historical data for {} with resolution {} from {} to {}", 
                    symbol, resolution, fromTimestamp, toTimestamp);
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // Check if the response contains valid data and no error
                if (jsonNode.has("s") && "ok".equals(jsonNode.get("s").asText()) && 
                    jsonNode.has("c") && jsonNode.get("c").isArray()) {
                    
                    List<CandleData> candles = parseHistoricalResponse(jsonNode);
                    
                    log.info("Retrieved {} historical data points for {} ({})", candles.size(), symbol, resolution);
                    return new HistoricalData(candles, symbol, resolution, fromTimestamp, toTimestamp);
                } else {
                    log.warn("Invalid historical data response for {}: {}", symbol, response);
                }
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded for historical data. Symbol: {}", symbol);
            } else {
                log.error("HTTP error fetching historical data for {}: {}", symbol, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error fetching historical data for {}: {}", symbol, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get historical data with common time ranges
     */
    public HistoricalData getHistoricalData(String symbol, String period) {
        long toTimestamp = Instant.now().getEpochSecond();
        long fromTimestamp;
        String resolution;
        
        switch (period.toUpperCase()) {
            case "1D":
                fromTimestamp = toTimestamp - (24 * 60 * 60); // 1 day ago
                resolution = "5"; // 5-minute intervals
                break;
            case "1W":
                fromTimestamp = toTimestamp - (7 * 24 * 60 * 60); // 1 week ago
                resolution = "15"; // 15-minute intervals
                break;
            case "1M":
                fromTimestamp = toTimestamp - (30L * 24 * 60 * 60); // 1 month ago
                resolution = "60"; // 1-hour intervals
                break;
            case "3M":
                fromTimestamp = toTimestamp - (90L * 24 * 60 * 60); // 3 months ago
                resolution = "D"; // Daily intervals
                break;
            case "1Y":
                fromTimestamp = toTimestamp - (365L * 24 * 60 * 60); // 1 year ago
                resolution = "D"; // Daily intervals
                break;
            default:
                log.warn("Unknown period: {}. Using 1 month default.", period);
                fromTimestamp = toTimestamp - (30L * 24 * 60 * 60);
                resolution = "D";
        }
        
        return getHistoricalData(symbol, resolution, fromTimestamp, toTimestamp);
    }
    
    private List<CandleData> parseHistoricalResponse(JsonNode jsonNode) {
        List<CandleData> candles = new ArrayList<>();
        
        JsonNode openArray = jsonNode.get("o");
        JsonNode highArray = jsonNode.get("h");
        JsonNode lowArray = jsonNode.get("l");
        JsonNode closeArray = jsonNode.get("c");
        JsonNode volumeArray = jsonNode.get("v");
        JsonNode timestampArray = jsonNode.get("t");
        
        if (openArray != null && openArray.isArray()) {
            int size = openArray.size();
            
            for (int i = 0; i < size; i++) {
                try {
                    BigDecimal open = BigDecimal.valueOf(openArray.get(i).asDouble())
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal high = BigDecimal.valueOf(highArray.get(i).asDouble())
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal low = BigDecimal.valueOf(lowArray.get(i).asDouble())
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal close = BigDecimal.valueOf(closeArray.get(i).asDouble())
                            .setScale(2, RoundingMode.HALF_UP);
                    long volume = volumeArray.get(i).asLong();
                    long timestamp = timestampArray.get(i).asLong();
                    
                    candles.add(new CandleData(open, high, low, close, volume, timestamp));
                } catch (Exception e) {
                    log.warn("Error parsing candle data at index {}: {}", i, e.getMessage());
                }
            }
        }
        
        return candles;
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
    
    /**
     * Company Basic Financials data class
     */
    public static class BasicFinancials {
        public final JsonNode metric;
        public final JsonNode series;
        public final String metricType;
        public final String symbol;
        
        public BasicFinancials(JsonNode metric, JsonNode series, String metricType, String symbol) {
            this.metric = metric;
            this.series = series;
            this.metricType = metricType;
            this.symbol = symbol;
        }
    }
    
    /**
     * Reported Financials data class for SEC filing data
     */
    public static class ReportedFinancials {
        public final JsonNode data;
        public final String symbol;
        public final int count;
        
        public ReportedFinancials(JsonNode data, String symbol, int count) {
            this.data = data;
            this.symbol = symbol;
            this.count = count;
        }
    }
    
    /**
     * Get basic financial metrics for a company
     */
    public BasicFinancials getBasicFinancials(String symbol) {
        try {
            // Rate limiting
            enforceRateLimit();
            
            String url = String.format("%s/stock/metric?symbol=%s&metric=all&token=%s", 
                    baseUrl, symbol.toUpperCase(), apiToken);
            
            log.debug("Fetching basic financials for {} from Finnhub", symbol);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // Check if the response contains valid data
                if (jsonNode.has("metric") && jsonNode.get("metric").isObject()) {
                    JsonNode metric = jsonNode.get("metric");
                    JsonNode series = jsonNode.has("series") ? jsonNode.get("series") : null;
                    String metricType = jsonNode.has("metricType") ? jsonNode.get("metricType").asText() : "all";
                    
                    log.info("Retrieved basic financials for {}", symbol);
                    return new BasicFinancials(metric, series, metricType, symbol);
                } else {
                    log.warn("Invalid financials response for symbol {}: {}", symbol, response);
                }
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded for basic financials. Symbol: {}", symbol);
            } else {
                log.error("HTTP error fetching basic financials for {}: {}", symbol, e.getMessage());
            }
        } catch (ResourceAccessException e) {
            log.error("Network error fetching basic financials for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching basic financials for {} from Finnhub: {}", symbol, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get reported financial statements for a company
     */
    public ReportedFinancials getReportedFinancials(String symbol) {
        try {
            // Rate limiting
            enforceRateLimit();
            
            String url = String.format("%s/stock/financials-reported?symbol=%s&token=%s", 
                    baseUrl, symbol.toUpperCase(), apiToken);
            
            log.debug("Fetching reported financials for {} from Finnhub", symbol);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // Check if the response contains valid data
                if (jsonNode.has("data") && jsonNode.get("data").isArray()) {
                    JsonNode data = jsonNode.get("data");
                    int count = data.size();
                    
                    log.info("Retrieved {} reported financial statements for {}", count, symbol);
                    return new ReportedFinancials(data, symbol, count);
                } else {
                    log.warn("Invalid reported financials response for symbol {}: {}", symbol, response);
                }
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded for reported financials. Symbol: {}", symbol);
            } else {
                log.error("HTTP error fetching reported financials for {}: {}", symbol, e.getMessage());
            }
        } catch (ResourceAccessException e) {
            log.error("Network error fetching reported financials for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching reported financials for {} from Finnhub: {}", symbol, e.getMessage());
        }
        
        return null;
    }
}