package com.stockbrokerage.service;

import com.stockbrokerage.entity.Wallet;
import com.stockbrokerage.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    
    private final WalletRepository walletRepository;
    
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }
    
    @Transactional
    public Wallet addBalance(String userId, BigDecimal amount, String paymentMethod) {
        log.info("Adding balance: {} for user: {} via payment method: {}", amount, userId, paymentMethod);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        
        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Balance added successfully. New balance: {} for user: {}", savedWallet.getBalance(), userId);
        
        return savedWallet;
    }
    
    @Transactional
    public Wallet subtractBalance(String userId, BigDecimal amount) {
        log.info("Subtracting balance: {} for user: {}", amount, userId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        
        Wallet wallet = getWalletByUserId(userId);
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        
        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Balance subtracted successfully. New balance: {} for user: {}", savedWallet.getBalance(), userId);
        
        return savedWallet;
    }
}