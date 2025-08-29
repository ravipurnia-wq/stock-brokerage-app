package com.stockbrokerage.kafka;

import com.stockbrokerage.entity.Holding;
import com.stockbrokerage.entity.Trade;
import com.stockbrokerage.entity.Transaction;
import com.stockbrokerage.entity.Wallet;
import com.stockbrokerage.events.PortfolioUpdateEvent;
import com.stockbrokerage.events.TradeExecutedEvent;
import com.stockbrokerage.repository.HoldingRepository;
import com.stockbrokerage.repository.TransactionRepository;
import com.stockbrokerage.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeEventConsumer {
    
    private final HoldingRepository holdingRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "trade-events", groupId = "portfolio-update-group")
    @Transactional
    public void processTradeEvent(TradeExecutedEvent event) {
        try {
            log.info("Processing trade event: {} for user: {}", event.getTradeId(), event.getUserId());
            
            // Create trade record
            createTradeRecord(event);
            
            // Update holdings
            updateHoldings(event);
            
            // Update wallet balance
            updateWalletBalance(event);
            
            // Create transaction record
            createTransactionRecord(event);
            
            // Publish portfolio update event
            publishPortfolioUpdateEvent(event);
            
            log.info("Trade processed successfully: {}", event.getTradeId());
            
        } catch (Exception e) {
            log.error("Error processing trade event: {}", event.getTradeId(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }
    
    private void createTradeRecord(TradeExecutedEvent event) {
        Trade trade = Trade.builder()
                .id(event.getTradeId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .symbolId(event.getSymbolId())
                .side(event.getSide())
                .quantity(event.getQuantity())
                .price(event.getExecutionPrice())
                .totalValue(event.getTotalValue())
                .fees(event.getFees())
                .executedAt(event.getExecutedAt())
                .build();
        
        // Note: You would save this to a Trade repository if it exists
        log.debug("Trade record created: {}", trade.getId());
    }
    
    private void updateHoldings(TradeExecutedEvent event) {
        Optional<Holding> existingHolding = holdingRepository
                .findByUserIdAndSymbolId(event.getUserId(), event.getSymbolId());
        
        if (event.getSide() == com.stockbrokerage.entity.Order.OrderSide.BUY) {
            // Buy order - increase holdings
            if (existingHolding.isPresent()) {
                Holding holding = existingHolding.get();
                Long newQuantity = holding.getQuantity() + event.getQuantity();
                BigDecimal newTotalCost = holding.getTotalCost().add(event.getTotalValue());
                BigDecimal newAveragePrice = newTotalCost.divide(BigDecimal.valueOf(newQuantity), 2, 
                    java.math.RoundingMode.HALF_UP);
                
                holding.setQuantity(newQuantity);
                holding.setAveragePrice(newAveragePrice);
                holding.setTotalCost(newTotalCost);
                holding.setUpdatedAt(LocalDateTime.now());
                
                holdingRepository.save(holding);
            } else {
                // Create new holding
                Holding holding = Holding.builder()
                        .userId(event.getUserId())
                        .symbolId(event.getSymbolId())
                        .quantity(event.getQuantity())
                        .averagePrice(event.getExecutionPrice())
                        .totalCost(event.getTotalValue())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                holdingRepository.save(holding);
            }
        } else {
            // Sell order - decrease holdings
            if (existingHolding.isPresent()) {
                Holding holding = existingHolding.get();
                Long newQuantity = holding.getQuantity() - event.getQuantity();
                
                if (newQuantity > 0) {
                    BigDecimal soldCost = holding.getAveragePrice().multiply(BigDecimal.valueOf(event.getQuantity()));
                    holding.setQuantity(newQuantity);
                    holding.setTotalCost(holding.getTotalCost().subtract(soldCost));
                    holding.setUpdatedAt(LocalDateTime.now());
                    
                    holdingRepository.save(holding);
                } else {
                    // Remove holding if quantity becomes zero or negative
                    holdingRepository.delete(holding);
                }
            }
        }
    }
    
    private void updateWalletBalance(TradeExecutedEvent event) {
        Optional<Wallet> walletOpt = walletRepository.findByUserId(event.getUserId());
        
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            BigDecimal balanceChange;
            
            if (event.getSide() == com.stockbrokerage.entity.Order.OrderSide.BUY) {
                // Buy order - decrease balance (amount + fees)
                balanceChange = event.getTotalValue().add(event.getFees()).negate();
            } else {
                // Sell order - increase balance (amount - fees)
                balanceChange = event.getTotalValue().subtract(event.getFees());
            }
            
            wallet.setBalance(wallet.getBalance().add(balanceChange));
            wallet.setUpdatedAt(LocalDateTime.now());
            
            walletRepository.save(wallet);
        }
    }
    
    private void createTransactionRecord(TradeExecutedEvent event) {
        String transactionType = event.getSide() == com.stockbrokerage.entity.Order.OrderSide.BUY ? 
                "STOCK_PURCHASE" : "STOCK_SALE";
        
        Transaction transaction = Transaction.builder()
                .userId(event.getUserId())
                .type(Transaction.TransactionType.valueOf(transactionType))
                .amount(event.getTotalValue())
                .fees(event.getFees())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Trade execution for " + event.getSymbol())
                .referenceId(event.getTradeId())
                .build();
        
        transactionRepository.save(transaction);
    }
    
    private void publishPortfolioUpdateEvent(TradeExecutedEvent event) {
        PortfolioUpdateEvent portfolioEvent = PortfolioUpdateEvent.builder()
                .userId(event.getUserId())
                .symbolId(event.getSymbolId())
                .symbol(event.getSymbol())
                .eventType("TRADE_EXECUTED")
                .quantityChange(event.getSide() == com.stockbrokerage.entity.Order.OrderSide.BUY ? 
                        event.getQuantity() : -event.getQuantity())
                .balanceChange(event.getSide() == com.stockbrokerage.entity.Order.OrderSide.BUY ? 
                        event.getTotalValue().add(event.getFees()).negate() : 
                        event.getTotalValue().subtract(event.getFees()))
                .timestamp(event.getExecutedAt())
                .build();
        
        kafkaTemplate.send("portfolio-updates", portfolioEvent);
    }
}