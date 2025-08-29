package com.stockbrokerage.repository;

import com.stockbrokerage.entity.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {
    
    Optional<Wallet> findByUserId(String userId);
    
    Optional<Wallet> findByUserIdAndCurrency(String userId, String currency);
}