package com.stockbrokerage.events;

import com.stockbrokerage.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent {
    
    private String orderId;
    private String userId;
    private String symbolId;
    private Order.OrderType orderType;
    private Order.OrderSide side;
    private Long quantity;
    private BigDecimal price;
}