package com.stockbrokerage.controller;

import com.stockbrokerage.dto.HoldingResponse;
import com.stockbrokerage.dto.PortfolioSummary;
import com.stockbrokerage.security.UserPrincipal;
import com.stockbrokerage.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPortfolio(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            PortfolioSummary portfolio = portfolioService.getPortfolioSummary(userPrincipal.getId());
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            log.error("Error getting portfolio", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get portfolio", "message", e.getMessage()));
        }
    }

    @GetMapping("/holdings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getHoldings(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<HoldingResponse> holdings = portfolioService.getHoldings(userPrincipal.getId());
            return ResponseEntity.ok(holdings);
        } catch (Exception e) {
            log.error("Error getting holdings", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get holdings", "message", e.getMessage()));
        }
    }
}