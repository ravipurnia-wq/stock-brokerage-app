package com.stockbrokerage.repository;

import com.stockbrokerage.entity.UserWatchlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWatchlistRepository extends MongoRepository<UserWatchlist, String> {
    
    List<UserWatchlist> findByUserIdAndIsActiveTrue(String userId);
    
    Optional<UserWatchlist> findByUserIdAndSymbol(String userId, String symbol);
    
    void deleteByUserIdAndSymbol(String userId, String symbol);
    
    boolean existsByUserIdAndSymbol(String userId, String symbol);
}