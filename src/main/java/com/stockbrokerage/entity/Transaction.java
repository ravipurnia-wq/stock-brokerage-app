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
@Document(collection = "transactions")
public class Transaction {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String transactionId;
    
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal fees;
    private TransactionStatus status;
    private String paymentMethod;
    private String description;
    private String referenceId;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, FEE, BUY_SETTLEMENT, SELL_SETTLEMENT, STOCK_PURCHASE, STOCK_SALE
    }
    
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
}