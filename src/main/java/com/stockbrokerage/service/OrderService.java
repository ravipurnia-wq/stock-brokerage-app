package com.stockbrokerage.service;

import com.stockbrokerage.dto.OrderRequest;
import com.stockbrokerage.dto.OrderResponse;
import com.stockbrokerage.entity.Order;
import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.entity.User;
import com.stockbrokerage.events.OrderPlacedEvent;
import com.stockbrokerage.repository.OrderRepository;
import com.stockbrokerage.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final SymbolRepository symbolRepository;
    private final UserService userService;
    private final PortfolioService portfolioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String userId) {
        // Validate user
        User user = userService.findById(userId);
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }
        
        // Validate symbol
        Symbol symbol = symbolRepository.findBySymbol(request.getSymbol())
                .orElseThrow(() -> new RuntimeException("Symbol not found: " + request.getSymbol()));
        
        // Validate order
        validateOrder(request);
        
        // Check sufficient funds/holdings
        validateSufficientFundsOrHoldings(request, userId, symbol);
        
        // Create order
        Order order = createOrder(request, userId, symbol.getId());
        
        // Lock funds/holdings
        lockFundsOrHoldings(order);
        
        // Save order
        order = orderRepository.save(order);
        
        // Publish order event to Kafka
        publishOrderEvent(order);
        
        log.info("Order placed successfully: {}", order.getId());
        return mapToOrderResponse(order, symbol.getSymbol());
    }
    
    private void validateOrder(OrderRequest request) {
        if (request.getOrderType() == Order.OrderType.LIMIT && request.getPrice() == null) {
            throw new RuntimeException("Price is required for limit orders");
        }
        
        if (request.getOrderType() == Order.OrderType.STOP_LOSS && request.getStopPrice() == null) {
            throw new RuntimeException("Stop price is required for stop-loss orders");
        }
        
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }
    }
    
    private void validateSufficientFundsOrHoldings(OrderRequest request, String userId, Symbol symbol) {
        if (request.getSide() == Order.OrderSide.BUY) {
            BigDecimal requiredAmount = calculateRequiredAmount(request);
            portfolioService.validateSufficientFunds(userId, requiredAmount);
        } else {
            portfolioService.validateSufficientHoldings(userId, symbol.getId(), request.getQuantity());
        }
    }
    
    private BigDecimal calculateRequiredAmount(OrderRequest request) {
        if (request.getOrderType() == Order.OrderType.MARKET) {
            // For market orders, we'll use a buffer (last price * 1.05)
            // In real implementation, this would be based on current market price
            return BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(request.getQuantity()));
        } else {
            return request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }
    }
    
    private Order createOrder(OrderRequest request, String userId, String symbolId) {
        BigDecimal orderValue = calculateRequiredAmount(request);
        BigDecimal fees = orderValue.multiply(BigDecimal.valueOf(0.001)); // 0.1% fee
        
        return Order.builder()
                .userId(userId)
                .symbolId(symbolId)
                .orderType(request.getOrderType())
                .side(request.getSide())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .stopPrice(request.getStopPrice())
                .status(Order.OrderStatus.PENDING)
                .filledQuantity(0L)
                .orderValue(orderValue)
                .fees(fees)
                .expiresAt(LocalDateTime.now().plusDays(30)) // 30 days expiry
                .build();
    }
    
    private void lockFundsOrHoldings(Order order) {
        if (order.getSide() == Order.OrderSide.BUY) {
            portfolioService.lockFunds(order.getUserId(), order.getOrderValue().add(order.getFees()));
        } else {
            portfolioService.lockHoldings(order.getUserId(), order.getSymbolId(), order.getQuantity());
        }
    }
    
    private void publishOrderEvent(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .symbolId(order.getSymbolId())
                .orderType(order.getOrderType())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .build();
        
        kafkaTemplate.send("order-events", event);
    }
    
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByPlacedAtDesc(userId, pageable);
        return orders.map(order -> {
            Symbol symbol = symbolRepository.findById(order.getSymbolId()).orElse(null);
            return mapToOrderResponse(order, symbol != null ? symbol.getSymbol() : "UNKNOWN");
        });
    }
    
    public OrderResponse getOrderById(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Symbol symbol = symbolRepository.findById(order.getSymbolId()).orElse(null);
        return mapToOrderResponse(order, symbol != null ? symbol.getSymbol() : "UNKNOWN");
    }
    
    @Transactional
    public void cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Release locked funds/holdings
        releaseLock(order);
        
        log.info("Order cancelled successfully: {}", orderId);
    }
    
    private void releaseLock(Order order) {
        if (order.getSide() == Order.OrderSide.BUY) {
            portfolioService.releaseFunds(order.getUserId(), order.getOrderValue().add(order.getFees()));
        } else {
            portfolioService.releaseHoldings(order.getUserId(), order.getSymbolId(), order.getQuantity());
        }
    }
    
    private OrderResponse mapToOrderResponse(Order order, String symbol) {
        return OrderResponse.builder()
                .id(order.getId())
                .symbol(symbol)
                .orderType(order.getOrderType())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .stopPrice(order.getStopPrice())
                .status(order.getStatus())
                .filledQuantity(order.getFilledQuantity())
                .filledPrice(order.getFilledPrice())
                .orderValue(order.getOrderValue())
                .fees(order.getFees())
                .placedAt(order.getPlacedAt())
                .filledAt(order.getFilledAt())
                .build();
    }
}