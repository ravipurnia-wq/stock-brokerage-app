package com.stockbrokerage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummary {
    
    private BigDecimal cashBalance;
    private BigDecimal lockedBalance;
    private BigDecimal totalInvestedValue;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalPnL;
    private BigDecimal pnLPercentage;
    private List<HoldingResponse> holdings;
}