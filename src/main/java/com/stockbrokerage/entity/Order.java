package com.stockbrokerage.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String symbolId;
    
    private OrderType orderType;
    private OrderSide side;
    private Long quantity;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private OrderStatus status;
    private Long filledQuantity;
    private BigDecimal filledPrice;
    private BigDecimal orderValue;
    private BigDecimal fees;
    
    @CreatedDate
    private LocalDateTime placedAt;
    
    private LocalDateTime filledAt;
    private LocalDateTime expiresAt;
    
    public enum OrderType {
        MARKET, LIMIT, STOP_LOSS, STOP_LIMIT
    }
    
    public enum OrderSide {
        BUY, SELL
    }
    
    public enum OrderStatus {
        PENDING, FILLED, PARTIALLY_FILLED, CANCELLED, REJECTED
    }
}