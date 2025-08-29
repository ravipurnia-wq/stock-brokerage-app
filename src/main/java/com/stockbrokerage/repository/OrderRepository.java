package com.stockbrokerage.repository;

import com.stockbrokerage.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    Page<Order> findByUserIdOrderByPlacedAtDesc(String userId, Pageable pageable);
    
    List<Order> findByUserIdAndStatus(String userId, Order.OrderStatus status);
    
    Optional<Order> findByIdAndUserId(String id, String userId);
    
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);
}