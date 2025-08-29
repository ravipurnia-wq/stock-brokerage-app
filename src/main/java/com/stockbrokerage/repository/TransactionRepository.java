package com.stockbrokerage.repository;

import com.stockbrokerage.entity.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<Transaction> findByIdAndUserId(String id, String userId);
    
    List<Transaction> findByUserIdAndType(String userId, Transaction.TransactionType type);
}