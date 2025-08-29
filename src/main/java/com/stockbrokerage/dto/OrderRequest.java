package com.stockbrokerage.dto;

import com.stockbrokerage.entity.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Order type is required")
    private Order.OrderType orderType;
    
    @NotNull(message = "Order side is required")
    private Order.OrderSide side;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Long quantity;
    
    private BigDecimal price;
    
    private BigDecimal stopPrice;
}