package com.stockbrokerage.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_kyc")
public class UserKyc {
    
    @Id
    private String id;
    
    private String userId;
    private DocumentType documentType;
    private String documentNumber;
    private String documentUrl;
    private VerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum DocumentType {
        PASSPORT, DRIVER_LICENSE, SSN, NATIONAL_ID
    }
    
    public enum VerificationStatus {
        PENDING, APPROVED, REJECTED
    }
}