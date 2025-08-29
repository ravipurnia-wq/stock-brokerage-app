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
public class MarketDataEvent {
    
    private String symbolId;
    private String symbol;
    private BigDecimal price;
    private Long volume;
    private LocalDateTime timestamp;
}