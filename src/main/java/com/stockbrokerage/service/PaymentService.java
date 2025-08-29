package com.stockbrokerage.service;

import com.stockbrokerage.dto.DepositRequest;
import com.stockbrokerage.dto.WithdrawRequest;
import com.stockbrokerage.entity.Transaction;
import com.stockbrokerage.entity.User;
import com.stockbrokerage.entity.Wallet;
import com.stockbrokerage.repository.TransactionRepository;
import com.stockbrokerage.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    
    @Transactional
    public Transaction depositFunds(String userId, DepositRequest request) {
        User user = userService.findById(userId);
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        
        // Simulate payment processing (in real world, integrate with payment gateway)
        String transactionId = processPayment(request);
        
        // Update wallet balance
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .transactionId(transactionId)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.COMPLETED)
                .paymentMethod(request.getPaymentMethod())
                .description("Deposit via " + request.getPaymentMethod())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        log.info("Deposit completed for user: {} amount: {} transaction: {}", 
                userId, request.getAmount(), transactionId);
        
        return transaction;
    }
    
    @Transactional
    public Transaction withdrawFunds(String userId, WithdrawRequest request) {
        User user = userService.findById(userId);
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be positive");
        }
        
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds for withdrawal");
        }
        
        // Simulate withdrawal processing
        String transactionId = processWithdrawal(request);
        
        // Update wallet balance
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .transactionId(transactionId)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.COMPLETED)
                .paymentMethod(request.getPaymentMethod())
                .description("Withdrawal via " + request.getPaymentMethod())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        log.info("Withdrawal completed for user: {} amount: {} transaction: {}", 
                userId, request.getAmount(), transactionId);
        
        return transaction;
    }
    
    public List<Transaction> getTransactionHistory(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Transaction getTransactionById(String transactionId, String userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
    
    @Transactional
    public void recordTradeFee(String userId, BigDecimal feeAmount, String orderId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        wallet.setBalance(wallet.getBalance().subtract(feeAmount));
        walletRepository.save(wallet);
        
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .transactionId(UUID.randomUUID().toString())
                .type(Transaction.TransactionType.FEE)
                .amount(feeAmount)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Trading fee for order " + orderId)
                .build();
        
        transactionRepository.save(transaction);
        
        log.info("Recorded trading fee for user: {} amount: {} order: {}", 
                userId, feeAmount, orderId);
    }
    
    @Transactional
    public void recordTradeSettlement(String userId, BigDecimal amount, 
                                     Transaction.TransactionType type, String orderId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        if (type == Transaction.TransactionType.BUY_SETTLEMENT) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else if (type == Transaction.TransactionType.SELL_SETTLEMENT) {
            wallet.setBalance(wallet.getBalance().add(amount));
        }
        
        walletRepository.save(wallet);
        
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .transactionId(UUID.randomUUID().toString())
                .type(type)
                .amount(amount)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Trade settlement for order " + orderId)
                .build();
        
        transactionRepository.save(transaction);
        
        log.info("Recorded trade settlement for user: {} amount: {} type: {} order: {}", 
                userId, amount, type, orderId);
    }
    
    private String processPayment(DepositRequest request) {
        // Simulate payment gateway integration
        // In real implementation, integrate with Stripe, PayPal, etc.
        
        try {
            // Simulate processing delay
            Thread.sleep(1000);
            
            // Simulate 95% success rate
            if (Math.random() < 0.95) {
                return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            } else {
                throw new RuntimeException("Payment processing failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
    }
    
    private String processWithdrawal(WithdrawRequest request) {
        // Simulate withdrawal processing
        // In real implementation, integrate with bank APIs
        
        try {
            // Simulate processing delay
            Thread.sleep(1000);
            
            // Simulate 98% success rate
            if (Math.random() < 0.98) {
                return "WTH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            } else {
                throw new RuntimeException("Withdrawal processing failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Withdrawal processing interrupted", e);
        }
    }
}