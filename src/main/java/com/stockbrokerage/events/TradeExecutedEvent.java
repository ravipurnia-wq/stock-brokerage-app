package com.stockbrokerage.events;

import com.stockbrokerage.entity.Order;
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
public class TradeExecutedEvent {
    
    private String tradeId;
    private String orderId;
    private String userId;
    private String symbolId;
    private String symbol;
    private Order.OrderSide side;
    private Long quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalValue;
    private BigDecimal fees;
    private LocalDateTime executedAt;
}