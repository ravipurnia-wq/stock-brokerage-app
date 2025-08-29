package com.stockbrokerage.service;

import com.stockbrokerage.dto.HoldingResponse;
import com.stockbrokerage.dto.PortfolioSummary;
import com.stockbrokerage.entity.Holding;
import com.stockbrokerage.entity.Order;
import com.stockbrokerage.entity.Symbol;
import com.stockbrokerage.entity.Wallet;
import com.stockbrokerage.repository.HoldingRepository;
import com.stockbrokerage.repository.SymbolRepository;
import com.stockbrokerage.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {
    
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final SymbolRepository symbolRepository;
    private final MarketDataService marketDataService;
    
    public PortfolioSummary getPortfolioSummary(String userId) {
        Wallet wallet = getWallet(userId);
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        
        BigDecimal totalInvestedValue = holdings.stream()
                .map(Holding::getTotalInvestment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCurrentValue = calculateTotalCurrentValue(holdings);
        BigDecimal totalPnL = totalCurrentValue.subtract(totalInvestedValue);
        BigDecimal pnLPercentage = totalInvestedValue.compareTo(BigDecimal.ZERO) > 0 
                ? totalPnL.divide(totalInvestedValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        List<HoldingResponse> holdingResponses = holdings.stream()
                .map(this::mapToHoldingResponse)
                .collect(Collectors.toList());
        
        return PortfolioSummary.builder()
                .cashBalance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .totalInvestedValue(totalInvestedValue)
                .totalCurrentValue(totalCurrentValue)
                .totalPnL(totalPnL)
                .pnLPercentage(pnLPercentage)
                .holdings(holdingResponses)
                .build();
    }
    
    public List<HoldingResponse> getHoldings(String userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        return holdings.stream()
                .map(this::mapToHoldingResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void updateHolding(String userId, String symbolId, Long quantity, 
                             BigDecimal price, Order.OrderSide side) {
        Holding holding = holdingRepository.findByUserIdAndSymbolId(userId, symbolId)
                .orElse(Holding.builder()
                        .userId(userId)
                        .symbolId(symbolId)
                        .quantity(0L)
                        .averagePrice(BigDecimal.ZERO)
                        .totalInvestment(BigDecimal.ZERO)
                        .build());
        
        if (side == Order.OrderSide.BUY) {
            // Calculate new average price
            BigDecimal currentValue = holding.getAveragePrice().multiply(BigDecimal.valueOf(holding.getQuantity()));
            BigDecimal newValue = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal totalValue = currentValue.add(newValue);
            Long totalQuantity = holding.getQuantity() + quantity;
            
            holding.setQuantity(totalQuantity);
            holding.setAveragePrice(totalValue.divide(BigDecimal.valueOf(totalQuantity), 4, RoundingMode.HALF_UP));
            holding.setTotalInvestment(totalValue);
            
            holdingRepository.save(holding);
        } else {
            // Reduce quantity
            Long newQuantity = holding.getQuantity() - quantity;
            if (newQuantity <= 0) {
                holdingRepository.deleteByUserIdAndSymbolId(userId, symbolId);
            } else {
                BigDecimal soldValue = price.multiply(BigDecimal.valueOf(quantity));
                BigDecimal remainingInvestment = holding.getTotalInvestment()
                        .subtract(holding.getAveragePrice().multiply(BigDecimal.valueOf(quantity)));
                
                holding.setQuantity(newQuantity);
                holding.setTotalInvestment(remainingInvestment);
                holdingRepository.save(holding);
            }
        }
        
        log.info("Updated holding for user: {} symbol: {} side: {} quantity: {}", 
                userId, symbolId, side, quantity);
    }
    
    public void validateSufficientFunds(String userId, BigDecimal requiredAmount) {
        Wallet wallet = getWallet(userId);
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
        
        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw new RuntimeException("Insufficient funds. Required: " + requiredAmount + 
                    ", Available: " + availableBalance);
        }
    }
    
    public void validateSufficientHoldings(String userId, String symbolId, Long requiredQuantity) {
        Holding holding = holdingRepository.findByUserIdAndSymbolId(userId, symbolId)
                .orElseThrow(() -> new RuntimeException("No holdings found for symbol"));
        
        if (holding.getQuantity() < requiredQuantity) {
            throw new RuntimeException("Insufficient holdings. Required: " + requiredQuantity + 
                    ", Available: " + holding.getQuantity());
        }
    }
    
    @Transactional
    public void lockFunds(String userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (availableBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds to lock");
        }
        
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);
        
        log.info("Locked funds for user: {} amount: {}", userId, amount);
    }
    
    @Transactional
    public void releaseFunds(String userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);
        
        log.info("Released funds for user: {} amount: {}", userId, amount);
    }
    
    @Transactional
    public void deductFunds(String userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);
        
        log.info("Deducted funds for user: {} amount: {}", userId, amount);
    }
    
    @Transactional
    public void addFunds(String userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        
        log.info("Added funds for user: {} amount: {}", userId, amount);
    }
    
    public void lockHoldings(String userId, String symbolId, Long quantity) {
        // In a real implementation, you would track locked holdings separately
        // For simplicity, we're just validating that holdings exist
        validateSufficientHoldings(userId, symbolId, quantity);
        log.info("Locked holdings for user: {} symbol: {} quantity: {}", userId, symbolId, quantity);
    }
    
    public void releaseHoldings(String userId, String symbolId, Long quantity) {
        log.info("Released holdings for user: {} symbol: {} quantity: {}", userId, symbolId, quantity);
    }
    
    private Wallet getWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user"));
    }
    
    private BigDecimal calculateTotalCurrentValue(List<Holding> holdings) {
        return holdings.stream()
                .map(this::calculateHoldingCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateHoldingCurrentValue(Holding holding) {
        BigDecimal currentPrice = marketDataService.getCurrentPrice(holding.getSymbolId());
        return currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
    }
    
    private HoldingResponse mapToHoldingResponse(Holding holding) {
        Symbol symbol = symbolRepository.findById(holding.getSymbolId()).orElse(null);
        BigDecimal currentPrice = marketDataService.getCurrentPrice(holding.getSymbolId());
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal pnL = currentValue.subtract(holding.getTotalInvestment());
        BigDecimal pnLPercentage = holding.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0 
                ? pnL.divide(holding.getTotalInvestment(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        return HoldingResponse.builder()
                .symbolId(holding.getSymbolId())
                .symbol(symbol != null ? symbol.getSymbol() : "UNKNOWN")
                .companyName(symbol != null ? symbol.getCompanyName() : "Unknown Company")
                .quantity(holding.getQuantity())
                .averagePrice(holding.getAveragePrice())
                .currentPrice(currentPrice)
                .totalInvestment(holding.getTotalInvestment())
                .currentValue(currentValue)
                .pnL(pnL)
                .pnLPercentage(pnLPercentage)
                .build();
    }
}