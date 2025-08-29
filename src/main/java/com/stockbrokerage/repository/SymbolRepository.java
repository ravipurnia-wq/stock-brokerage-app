package com.stockbrokerage.repository;

import com.stockbrokerage.entity.Symbol;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymbolRepository extends MongoRepository<Symbol, String> {
    
    Optional<Symbol> findBySymbol(String symbol);
    
    List<Symbol> findByIsActiveTrue();
    
    List<Symbol> findByExchange(String exchange);
}