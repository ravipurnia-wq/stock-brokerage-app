package com.stockbrokerage.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;

@Document(collection = "user_watchlists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(def = "{'userId': 1, 'symbol': 1}", unique = true)
public class UserWatchlist {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String symbol;
    private String companyName;
    private String exchange;
    private String sector;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private LocalDateTime addedAt;
    
    
    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }
}