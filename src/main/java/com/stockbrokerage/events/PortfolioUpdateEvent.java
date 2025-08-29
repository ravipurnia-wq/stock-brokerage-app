package com.stockbrokerage.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioUpdateEvent {
    
    private String userId;
    private String symbolId;
    private String symbol;
    private String eventType; // TRADE_EXECUTED, DEPOSIT, WITHDRAWAL
    private Long quantityChange;
    private BigDecimal balanceChange;
    private BigDecimal newBalance;
    private LocalDateTime timestamp;
}