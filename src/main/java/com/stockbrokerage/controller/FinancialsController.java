package com.stockbrokerage.controller;

import com.stockbrokerage.service.FinnhubApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/financials")
@RequiredArgsConstructor
@Slf4j
public class FinancialsController {

    private final FinnhubApiService finnhubApiService;

    @GetMapping("/basic/{symbol}")
    public ResponseEntity<?> getBasicFinancials(@PathVariable String symbol) {
        try {
            log.debug("Fetching basic financials for symbol: {}", symbol);
            
            FinnhubApiService.BasicFinancials financials = finnhubApiService.getBasicFinancials(symbol);
            
            if (financials == null || financials.metric == null) {
                return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "data", Map.of(),
                    "message", "No financial data available for this symbol"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "symbol", financials.symbol,
                "metricType", financials.metricType,
                "metric", financials.metric,
                "series", financials.series != null ? financials.series : Map.of()
            ));

        } catch (Exception e) {
            log.error("Error fetching basic financials for symbol: {}", symbol, e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Failed to fetch financial data",
                    "message", e.getMessage(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/metrics/{symbol}")
    public ResponseEntity<?> getKeyMetrics(@PathVariable String symbol) {
        try {
            log.debug("Fetching key metrics for symbol: {}", symbol);
            
            FinnhubApiService.BasicFinancials financials = finnhubApiService.getBasicFinancials(symbol);
            
            if (financials == null || financials.metric == null) {
                return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "metrics", Map.of(),
                    "message", "No financial data available for this symbol"
                ));
            }

            // Extract key metrics for display
            Map<String, Object> keyMetrics = Map.of(
                "marketCap", financials.metric.has("marketCapitalization") ? 
                    financials.metric.get("marketCapitalization").asDouble() : 0.0,
                "peRatio", financials.metric.has("peTTM") ? 
                    financials.metric.get("peTTM").asDouble() : 0.0,
                "pbRatio", financials.metric.has("pb") ? 
                    financials.metric.get("pb").asDouble() : 0.0,
                "eps", financials.metric.has("epsTTM") ? 
                    financials.metric.get("epsTTM").asDouble() : 0.0,
                "dividendYield", financials.metric.has("currentDividendYieldTTM") ? 
                    financials.metric.get("currentDividendYieldTTM").asDouble() : 0.0,
                "roe", financials.metric.has("roeTTM") ? 
                    financials.metric.get("roeTTM").asDouble() : 0.0,
                "roa", financials.metric.has("roaTTM") ? 
                    financials.metric.get("roaTTM").asDouble() : 0.0,
                "beta", financials.metric.has("beta") ? 
                    financials.metric.get("beta").asDouble() : 0.0,
                "52WeekHigh", financials.metric.has("52WeekHigh") ? 
                    financials.metric.get("52WeekHigh").asDouble() : 0.0,
                "52WeekLow", financials.metric.has("52WeekLow") ? 
                    financials.metric.get("52WeekLow").asDouble() : 0.0
            );

            return ResponseEntity.ok(Map.of(
                "symbol", financials.symbol,
                "metrics", keyMetrics
            ));

        } catch (Exception e) {
            log.error("Error fetching key metrics for symbol: {}", symbol, e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Failed to fetch key metrics",
                    "message", e.getMessage(),
                    "symbol", symbol
                ));
        }
    }
}