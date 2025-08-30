package com.stockbrokerage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.entity.UserWatchlist;
import com.stockbrokerage.repository.SymbolRepository;
import com.stockbrokerage.repository.UserWatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSearchService {
    
    private final SymbolRepository symbolRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final MarketDataService marketDataService;
    private final RestTemplate restTemplate;
    private final FinnhubApiService finnhubApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Popular stock symbols for search suggestions
    private static final Map<String, Map<String, String>> POPULAR_STOCKS = new HashMap<>();
    
    static {
        POPULAR_STOCKS.put("AAPL", Map.of("name", "Apple Inc.", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("GOOGL", Map.of("name", "Alphabet Inc.", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("MSFT", Map.of("name", "Microsoft Corporation", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("AMZN", Map.of("name", "Amazon.com Inc.", "exchange", "NASDAQ", "sector", "E-commerce"));
        POPULAR_STOCKS.put("TSLA", Map.of("name", "Tesla Inc.", "exchange", "NASDAQ", "sector", "Automotive"));
        POPULAR_STOCKS.put("META", Map.of("name", "Meta Platforms Inc.", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("NFLX", Map.of("name", "Netflix Inc.", "exchange", "NASDAQ", "sector", "Entertainment"));
        POPULAR_STOCKS.put("NVDA", Map.of("name", "NVIDIA Corporation", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("JPM", Map.of("name", "JPMorgan Chase & Co.", "exchange", "NYSE", "sector", "Financial Services"));
        POPULAR_STOCKS.put("V", Map.of("name", "Visa Inc.", "exchange", "NYSE", "sector", "Financial Services"));
        POPULAR_STOCKS.put("JNJ", Map.of("name", "Johnson & Johnson", "exchange", "NYSE", "sector", "Healthcare"));
        POPULAR_STOCKS.put("WMT", Map.of("name", "Walmart Inc.", "exchange", "NYSE", "sector", "Consumer Defensive"));
        POPULAR_STOCKS.put("PG", Map.of("name", "Procter & Gamble Co.", "exchange", "NYSE", "sector", "Consumer Defensive"));
        POPULAR_STOCKS.put("UNH", Map.of("name", "UnitedHealth Group Inc.", "exchange", "NYSE", "sector", "Healthcare"));
        POPULAR_STOCKS.put("HD", Map.of("name", "The Home Depot Inc.", "exchange", "NYSE", "sector", "Consumer Cyclical"));
        POPULAR_STOCKS.put("BAC", Map.of("name", "Bank of America Corp", "exchange", "NYSE", "sector", "Financial Services"));
        POPULAR_STOCKS.put("MA", Map.of("name", "Mastercard Inc.", "exchange", "NYSE", "sector", "Financial Services"));
        POPULAR_STOCKS.put("DIS", Map.of("name", "The Walt Disney Company", "exchange", "NYSE", "sector", "Entertainment"));
        POPULAR_STOCKS.put("ADBE", Map.of("name", "Adobe Inc.", "exchange", "NASDAQ", "sector", "Technology"));
        POPULAR_STOCKS.put("CRM", Map.of("name", "Salesforce Inc.", "exchange", "NYSE", "sector", "Technology"));
    }
    
    public List<Map<String, Object>> searchStocks(String query, String userId) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        
        String searchQuery = query.trim().toUpperCase();
        
        // Search in popular stocks first
        for (Map.Entry<String, Map<String, String>> entry : POPULAR_STOCKS.entrySet()) {
            String symbol = entry.getKey();
            Map<String, String> info = entry.getValue();
            
            // Check if symbol matches or company name contains the search query
            if (symbol.contains(searchQuery) || 
                info.get("name").toUpperCase().contains(searchQuery)) {
                
                Map<String, Object> result = new HashMap<>();
                result.put("symbol", symbol);
                result.put("companyName", info.get("name"));
                result.put("exchange", info.get("exchange"));
                result.put("sector", info.get("sector"));
                result.put("isAdded", isStockAlreadyAddedByUser(symbol, userId));
                
                results.add(result);
            }
        }
        
        // Sort results - exact symbol matches first, then by name
        results.sort((a, b) -> {
            String symbolA = (String) a.get("symbol");
            String symbolB = (String) b.get("symbol");
            
            if (symbolA.equals(searchQuery)) return -1;
            if (symbolB.equals(searchQuery)) return 1;
            
            return symbolA.compareTo(symbolB);
        });
        
        // Limit to top 10 results
        return results.subList(0, Math.min(results.size(), 10));
    }
    
    public UserWatchlist addStockToUserWatchlist(String symbol, String companyName, String exchange, String sector, String userId) {
        // Check if user already has this stock in watchlist
        if (userWatchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new RuntimeException("Stock already exists in your watchlist");
        }
        
        // Ensure the global symbol exists for price tracking
        Optional<Symbol> existing = symbolRepository.findBySymbol(symbol);
        Symbol globalSymbol;
        if (existing.isEmpty()) {
            // Create global symbol for price tracking if it doesn't exist
            globalSymbol = Symbol.builder()
                    .symbol(symbol.toUpperCase())
                    .companyName(companyName)
                    .exchange(exchange)
                    .sector(sector)
                    .isActive(true)
                    .build();
            globalSymbol = symbolRepository.save(globalSymbol);
            
            // Initialize with a starting price
            initializeStockPrice(globalSymbol);
        } else {
            globalSymbol = existing.get();
            if (!globalSymbol.getIsActive()) {
                globalSymbol.setIsActive(true);
                globalSymbol = symbolRepository.save(globalSymbol);
                initializeStockPrice(globalSymbol);
            }
        }
        
        // Add to user's watchlist
        UserWatchlist userWatchlist = UserWatchlist.builder()
                .userId(userId)
                .symbol(symbol.toUpperCase())
                .companyName(companyName)
                .exchange(exchange)
                .sector(sector)
                .isActive(true)
                .addedAt(java.time.LocalDateTime.now())
                .build();
        
        UserWatchlist savedWatchlist = userWatchlistRepository.save(userWatchlist);
        
        log.info("Added stock {} to user {}'s watchlist", symbol, userId);
        return savedWatchlist;
    }
    
    public void removeStockFromUserWatchlist(String symbol, String userId) {
        Optional<UserWatchlist> watchlistItem = userWatchlistRepository.findByUserIdAndSymbol(userId, symbol);
        if (watchlistItem.isEmpty()) {
            throw new RuntimeException("Stock not found in your watchlist");
        }
        
        userWatchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
        
        log.info("Removed stock {} from user {}'s watchlist", symbol, userId);
    }
    
    public List<UserWatchlist> getUserWatchlist(String userId) {
        return userWatchlistRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    private boolean isStockAlreadyAddedByUser(String symbol, String userId) {
        return userWatchlistRepository.existsByUserIdAndSymbol(userId, symbol);
    }
    
    private void initializeStockPrice(Symbol symbol) {
        try {
            // Try to fetch real price first
            BigDecimal price = fetchRealPrice(symbol.getSymbol());
            if (price == null) {
                // Use default price range based on popular stocks
                price = generateInitialPrice(symbol.getSymbol());
            }
            
            marketDataService.updatePrice(symbol.getId(), price);
            log.info("Initialized price for {}: ${}", symbol.getSymbol(), price);
            
        } catch (Exception e) {
            log.warn("Failed to initialize price for {}: {}", symbol.getSymbol(), e.getMessage());
            // Set a default price
            BigDecimal defaultPrice = BigDecimal.valueOf(100.00);
            marketDataService.updatePrice(symbol.getId(), defaultPrice);
        }
    }
    
    private BigDecimal fetchRealPrice(String symbol) {
        try {
            // Try Finnhub API first for real-time data
            BigDecimal price = finnhubApiService.getCurrentPrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Fetched real-time price from Finnhub for {}: ${}", symbol, price);
                return price;
            }
        } catch (Exception e) {
            log.debug("Finnhub API failed for {}: {}", symbol, e.getMessage());
        }
        
        try {
            // Fallback to Yahoo Finance API
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s", symbol);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode chart = root.path("chart").path("result").get(0);
                JsonNode meta = chart.path("meta");
                
                if (meta.has("regularMarketPrice")) {
                    double price = meta.path("regularMarketPrice").asDouble();
                    log.debug("Fetched price from Yahoo Finance for {}: ${}", symbol, price);
                    return BigDecimal.valueOf(price).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to fetch real price for {}: {}", symbol, e.getMessage());
        }
        
        return null;
    }
    
    private BigDecimal generateInitialPrice(String symbol) {
        // Generate realistic starting prices based on symbol
        Random random = new Random();
        double basePrice;
        
        // Set base prices for different types of stocks
        if (POPULAR_STOCKS.containsKey(symbol)) {
            Map<String, String> info = POPULAR_STOCKS.get(symbol);
            String sector = info.get("sector");
            
            switch (sector) {
                case "Technology":
                    basePrice = 100 + random.nextDouble() * 400; // $100-$500
                    break;
                case "Financial Services":
                    basePrice = 80 + random.nextDouble() * 220; // $80-$300
                    break;
                case "Healthcare":
                    basePrice = 90 + random.nextDouble() * 260; // $90-$350
                    break;
                case "Automotive":
                    basePrice = 150 + random.nextDouble() * 350; // $150-$500
                    break;
                default:
                    basePrice = 60 + random.nextDouble() * 190; // $60-$250
            }
        } else {
            // For unknown stocks, use a moderate range
            basePrice = 50 + random.nextDouble() * 200; // $50-$250
        }
        
        return BigDecimal.valueOf(basePrice).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}