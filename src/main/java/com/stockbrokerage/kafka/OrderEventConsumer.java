package com.stockbrokerage.kafka;

import com.stockbrokerage.entity.Order;
import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.events.OrderPlacedEvent;
import com.stockbrokerage.events.TradeExecutedEvent;
import com.stockbrokerage.repository.OrderRepository;
import com.stockbrokerage.repository.SymbolRepository;
import com.stockbrokerage.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final OrderRepository orderRepository;
    private final SymbolRepository symbolRepository;
    private final MarketDataService marketDataService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "order-events", groupId = "order-processing-group")
    public void processOrderEvent(OrderPlacedEvent event) {
        try {
            log.info("Processing order event: {}", event.getOrderId());
            
            // Get the order from database
            Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
            if (orderOpt.isEmpty()) {
                log.error("Order not found: {}", event.getOrderId());
                return;
            }
            
            Order order = orderOpt.get();
            
            // Get symbol information
            Optional<Symbol> symbolOpt = symbolRepository.findById(event.getSymbolId());
            if (symbolOpt.isEmpty()) {
                log.error("Symbol not found: {}", event.getSymbolId());
                return;
            }
            
            Symbol symbol = symbolOpt.get();
            
            // Execute trade based on order type
            executeOrder(order, symbol);
            
        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }
    
    private void executeOrder(Order order, Symbol symbol) {
        try {
            BigDecimal executionPrice = getExecutionPrice(order, symbol);
            
            if (executionPrice == null) {
                log.warn("Could not determine execution price for order: {}", order.getId());
                return;
            }
            
            // Update order status
            order.setStatus(Order.OrderStatus.FILLED);
            order.setFilledQuantity(order.getQuantity());
            order.setFilledPrice(executionPrice);
            order.setFilledAt(LocalDateTime.now());
            
            orderRepository.save(order);
            
            // Calculate total value and fees
            BigDecimal totalValue = executionPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
            
            // Publish trade executed event
            TradeExecutedEvent tradeEvent = TradeExecutedEvent.builder()
                    .tradeId(UUID.randomUUID().toString())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .symbolId(order.getSymbolId())
                    .symbol(symbol.getSymbol())
                    .side(order.getSide())
                    .quantity(order.getQuantity())
                    .executionPrice(executionPrice)
                    .totalValue(totalValue)
                    .fees(order.getFees())
                    .executedAt(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send("trade-events", tradeEvent);
            
            log.info("Order executed successfully: {} at price: {}", order.getId(), executionPrice);
            
        } catch (Exception e) {
            log.error("Error executing order: {}", order.getId(), e);
            
            // Mark order as rejected on error
            order.setStatus(Order.OrderStatus.REJECTED);
            orderRepository.save(order);
        }
    }
    
    private BigDecimal getExecutionPrice(Order order, Symbol symbol) {
        if (order.getOrderType() == Order.OrderType.MARKET) {
            // For market orders, use current market price
            return marketDataService.getCurrentPrice(symbol.getId());
        } else if (order.getOrderType() == Order.OrderType.LIMIT) {
            // For limit orders, check if limit price can be executed
            BigDecimal currentPrice = marketDataService.getCurrentPrice(symbol.getId());
            if (currentPrice != null) {
                if (order.getSide() == Order.OrderSide.BUY && currentPrice.compareTo(order.getPrice()) <= 0) {
                    return order.getPrice();
                } else if (order.getSide() == Order.OrderSide.SELL && currentPrice.compareTo(order.getPrice()) >= 0) {
                    return order.getPrice();
                }
            }
        }
        
        return null; // Cannot execute at this time
    }
}