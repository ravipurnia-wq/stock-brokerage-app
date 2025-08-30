package com.stockbrokerage.controller;

import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.entity.UserWatchlist;
import com.stockbrokerage.events.MarketDataEvent;
import com.stockbrokerage.security.UserPrincipal;
import com.stockbrokerage.service.MarketDataService;
import com.stockbrokerage.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Slf4j
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final StockSearchService stockSearchService;

    @GetMapping("/symbols")
    public ResponseEntity<?> getAllSymbols() {
        try {
            List<Symbol> symbols = marketDataService.getAllActiveSymbols();
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            log.error("Error getting symbols", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get symbols", "message", e.getMessage()));
        }
    }

    @GetMapping("/data/{symbolId}")
    public ResponseEntity<?> getMarketData(@PathVariable String symbolId) {
        try {
            MarketDataEvent marketData = marketDataService.getMarketData(symbolId);
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            log.error("Error getting market data", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get market data", "message", e.getMessage()));
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<?> initializeMarketData() {
        try {
            marketDataService.initializeMarketData();
            return ResponseEntity.ok(Map.of("message", "Market data initialized successfully"));
        } catch (Exception e) {
            log.error("Error initializing market data", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to initialize market data", "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchStocks(@RequestParam String query) {
        try {
            List<Map<String, Object>> results = stockSearchService.searchStocks(query, "default");
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching stocks", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to search stocks", "message", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addStock(@RequestBody Map<String, String> request) {
        try {
            String symbol = request.get("symbol");
            String companyName = request.get("companyName");
            String exchange = request.get("exchange");
            String sector = request.get("sector");
            
            UserWatchlist newWatchlistItem = stockSearchService.addStockToUserWatchlist(symbol, companyName, exchange, sector, "default");
            return ResponseEntity.ok(newWatchlistItem);
        } catch (Exception e) {
            log.error("Error adding stock", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to add stock", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{symbol}")
    public ResponseEntity<?> removeStock(@PathVariable String symbol) {
        try {
            stockSearchService.removeStockFromUserWatchlist(symbol, "default");
            return ResponseEntity.ok(Map.of("message", "Stock removed from watchlist successfully"));
        } catch (Exception e) {
            log.error("Error removing stock", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to remove stock", "message", e.getMessage()));
        }
    }
}