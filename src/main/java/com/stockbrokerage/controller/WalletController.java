package com.stockbrokerage.controller;

import com.paypal.orders.Order;
import com.stockbrokerage.entity.Wallet;
import com.stockbrokerage.security.UserPrincipal;
import com.stockbrokerage.service.PayPalService;
import com.stockbrokerage.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;
    private final PayPalService payPalService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getWallet(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Wallet wallet = walletService.getWalletByUserId(userPrincipal.getId());
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            log.error("Error getting wallet", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get wallet", "message", e.getMessage()));
        }
    }

    @PostMapping("/add-balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addBalance(@RequestBody Map<String, Object> request,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String paymentMethod = request.get("paymentMethod").toString();
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Amount must be greater than 0"));
            }
            
            // For now, just add the balance directly
            // In a real implementation, you would process the payment
            walletService.addBalance(userPrincipal.getId(), amount, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Balance added successfully",
                    "amount", amount,
                    "paymentMethod", paymentMethod
            ));
        } catch (Exception e) {
            log.error("Error adding balance", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to add balance", "message", e.getMessage()));
        }
    }

    @PostMapping("/paypal/create-order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPayPalOrder(@RequestBody Map<String, Object> request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Amount must be greater than 0"));
            }
            
            Order order = payPalService.createAddBalanceOrder(amount, userPrincipal.getId());
            
            return ResponseEntity.ok(Map.of(
                    "orderId", order.id(),
                    "status", order.status()
            ));
        } catch (Exception e) {
            log.error("Error creating PayPal order", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create PayPal order", "message", e.getMessage()));
        }
    }

    @PostMapping("/paypal/capture-order/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> capturePayPalOrder(@PathVariable String orderId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Order order = payPalService.captureOrder(orderId);
            
            if ("COMPLETED".equals(order.status())) {
                // Extract amount from the order
                BigDecimal amount = new BigDecimal(order.purchaseUnits().get(0).amountWithBreakdown().value());
                
                // Add balance to user's wallet
                walletService.addBalance(userPrincipal.getId(), amount, "PAYPAL");
                
                return ResponseEntity.ok(Map.of(
                        "message", "Payment completed and balance added successfully",
                        "orderId", orderId,
                        "amount", amount,
                        "status", order.status()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Payment not completed", "status", order.status()));
            }
        } catch (Exception e) {
            log.error("Error capturing PayPal order", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to capture PayPal order", "message", e.getMessage()));
        }
    }
}