package com.stockbrokerage.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "symbols")
public class Symbol {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String symbol;
    
    private String companyName;
    private String exchange;
    private String sector;
    private Boolean isActive;
    
    @CreatedDate
    private LocalDateTime createdAt;
}