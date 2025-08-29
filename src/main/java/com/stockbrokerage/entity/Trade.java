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
@Document(collection = "trades")
public class Trade {
    
    @Id
    private String id;
    
    @Indexed
    private String orderId;
    
    @Indexed
    private String symbolId;
    
    @Indexed
    private String userId;
    
    private Order.OrderSide side;
    private Long quantity;
    private BigDecimal price;
    private BigDecimal tradeValue;
    private BigDecimal fees;
    
    @CreatedDate
    private LocalDateTime executedAt;
}