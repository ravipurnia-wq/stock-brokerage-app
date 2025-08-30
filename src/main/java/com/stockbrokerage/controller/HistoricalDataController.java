package com.stockbrokerage.controller;

import com.stockbrokerage.service.FinnhubApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/historical")
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataController {

    private final FinnhubApiService finnhubApiService;

    @GetMapping("/chart/{symbol}")
    public ResponseEntity<?> getHistoricalChart(@PathVariable String symbol,
                                              @RequestParam(defaultValue = "1M") String period) {
        try {
            log.debug("Fetching historical chart data for symbol: {} with period: {}", symbol, period);
            
            FinnhubApiService.HistoricalData historicalData = finnhubApiService.getHistoricalData(symbol, period);
            
            if (historicalData == null || historicalData.candles.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "period", period,
                    "data", new Object[0],
                    "message", "No historical data available for this symbol"
                ));
            }

            // Transform data for frontend chart consumption
            Object[] chartData = historicalData.candles.stream()
                .map(candle -> Map.of(
                    "timestamp", candle.timestamp * 1000, // Convert to milliseconds for JavaScript
                    "open", candle.open,
                    "high", candle.high,
                    "low", candle.low,
                    "close", candle.close,
                    "volume", candle.volume
                ))
                .toArray();

            return ResponseEntity.ok(Map.of(
                "symbol", historicalData.symbol,
                "period", period,
                "resolution", historicalData.resolution,
                "data", chartData,
                "count", chartData.length
            ));

        } catch (Exception e) {
            log.error("Error fetching historical chart data for symbol: {}", symbol, e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Failed to fetch historical data",
                    "message", e.getMessage(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/chart/{symbol}/custom")
    public ResponseEntity<?> getCustomHistoricalChart(@PathVariable String symbol,
                                                    @RequestParam String resolution,
                                                    @RequestParam long from,
                                                    @RequestParam long to) {
        try {
            log.debug("Fetching custom historical chart data for symbol: {} from {} to {} with resolution: {}", 
                    symbol, from, to, resolution);
            
            FinnhubApiService.HistoricalData historicalData = finnhubApiService.getHistoricalData(
                symbol, resolution, from, to);
            
            if (historicalData == null || historicalData.candles.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "resolution", resolution,
                    "from", from,
                    "to", to,
                    "data", new Object[0],
                    "message", "No historical data available for the specified time range"
                ));
            }

            // Transform data for frontend chart consumption
            Object[] chartData = historicalData.candles.stream()
                .map(candle -> Map.of(
                    "timestamp", candle.timestamp * 1000, // Convert to milliseconds for JavaScript
                    "open", candle.open,
                    "high", candle.high,
                    "low", candle.low,
                    "close", candle.close,
                    "volume", candle.volume
                ))
                .toArray();

            return ResponseEntity.ok(Map.of(
                "symbol", historicalData.symbol,
                "resolution", historicalData.resolution,
                "from", historicalData.fromTimestamp,
                "to", historicalData.toTimestamp,
                "data", chartData,
                "count", chartData.length
            ));

        } catch (Exception e) {
            log.error("Error fetching custom historical chart data for symbol: {}", symbol, e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Failed to fetch historical data",
                    "message", e.getMessage(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/periods")
    public ResponseEntity<?> getAvailablePeriods() {
        return ResponseEntity.ok(Map.of(
            "periods", new Object[]{
                Map.of("label", "1 Day", "value", "1D", "description", "5-minute intervals"),
                Map.of("label", "1 Week", "value", "1W", "description", "15-minute intervals"),
                Map.of("label", "1 Month", "value", "1M", "description", "1-hour intervals"),
                Map.of("label", "3 Months", "value", "3M", "description", "Daily intervals"),
                Map.of("label", "1 Year", "value", "1Y", "description", "Daily intervals")
            }
        ));
    }
}