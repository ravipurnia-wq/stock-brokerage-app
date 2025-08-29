package com.stockbrokerage.controller;

import com.stockbrokerage.dto.OrderRequest;
import com.stockbrokerage.dto.OrderResponse;
import com.stockbrokerage.security.UserPrincipal;
import com.stockbrokerage.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest request,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            OrderResponse order = orderService.placeOrder(request, userPrincipal.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error placing order", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order placement failed", "message", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserOrders(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderResponse> orders = orderService.getUserOrders(userPrincipal.getId(), pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting user orders", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get orders", "message", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getOrderById(@PathVariable String orderId,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            OrderResponse order = orderService.getOrderById(orderId, userPrincipal.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error getting order by id", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order not found", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            orderService.cancelOrder(orderId, userPrincipal.getId());
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling order", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order cancellation failed", "message", e.getMessage()));
        }
    }
}