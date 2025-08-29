package com.stockbrokerage.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;
    
    private UserStatus status;
    private KycStatus kycStatus;
    
    private Set<Role> roles;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum UserStatus {
        PENDING, ACTIVE, SUSPENDED, CLOSED
    }
    
    public enum KycStatus {
        NOT_STARTED, IN_PROGRESS, APPROVED, REJECTED
    }
    
    public enum Role {
        USER, ADMIN, TRADER
    }
}