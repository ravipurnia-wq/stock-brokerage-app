package com.stockbrokerage.config;

import com.stockbrokerage.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MarketDataService marketDataService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");
        
        try {
            // Initialize market data and symbols
            marketDataService.initializeMarketData();
            log.info("Market data initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
            // Don't let initialization errors prevent startup
        }
    }
}