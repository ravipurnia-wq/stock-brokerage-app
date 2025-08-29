package com.stockbrokerage.dto;

import com.stockbrokerage.entity.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    
    private String id;
    private String symbol;
    private Order.OrderType orderType;
    private Order.OrderSide side;
    private Long quantity;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private Order.OrderStatus status;
    private Long filledQuantity;
    private BigDecimal filledPrice;
    private BigDecimal orderValue;
    private BigDecimal fees;
    private LocalDateTime placedAt;
    private LocalDateTime filledAt;
}