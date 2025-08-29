package com.stockbrokerage.repository;

import com.stockbrokerage.entity.Holding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends MongoRepository<Holding, String> {
    
    List<Holding> findByUserId(String userId);
    
    Optional<Holding> findByUserIdAndSymbolId(String userId, String symbolId);
    
    void deleteByUserIdAndSymbolId(String userId, String symbolId);
}