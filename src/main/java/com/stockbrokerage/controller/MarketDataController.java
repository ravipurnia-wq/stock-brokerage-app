package com.stockbrokerage.controller;

import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.events.MarketDataEvent;
import com.stockbrokerage.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Slf4j
public class MarketDataController {

    private final MarketDataService marketDataService;

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
}