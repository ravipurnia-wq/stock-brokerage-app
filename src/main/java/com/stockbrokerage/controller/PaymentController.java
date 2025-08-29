package com.stockbrokerage.controller;

import com.stockbrokerage.dto.DepositRequest;
import com.stockbrokerage.dto.WithdrawRequest;
import com.stockbrokerage.entity.Transaction;
import com.stockbrokerage.security.UserPrincipal;
import com.stockbrokerage.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> depositFunds(@Valid @RequestBody DepositRequest request,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Transaction transaction = paymentService.depositFunds(userPrincipal.getId(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Deposit successful",
                    "transaction", transaction
            ));
        } catch (Exception e) {
            log.error("Error processing deposit", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Deposit failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> withdrawFunds(@Valid @RequestBody WithdrawRequest request,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Transaction transaction = paymentService.withdrawFunds(userPrincipal.getId(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Withdrawal successful",
                    "transaction", transaction
            ));
        } catch (Exception e) {
            log.error("Error processing withdrawal", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Withdrawal failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTransactionHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<Transaction> transactions = paymentService.getTransactionHistory(userPrincipal.getId());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transaction history", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get transactions", "message", e.getMessage()));
        }
    }

    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTransactionById(@PathVariable String transactionId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Transaction transaction = paymentService.getTransactionById(transactionId, userPrincipal.getId());
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            log.error("Error getting transaction by id", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Transaction not found", "message", e.getMessage()));
        }
    }
}