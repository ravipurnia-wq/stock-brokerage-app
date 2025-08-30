package com.stockbrokerage.service;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PayPalService {
    
    private final PayPalHttpClient payPalClient;
    
    @Value("${paypal.client.id}")
    private String clientId;
    
    @Value("${paypal.client.secret}")
    private String clientSecret;
    
    @Value("${paypal.mode}")
    private String mode;
    
    @Value("${paypal.return.success.url}")
    private String successUrl;
    
    @Value("${paypal.return.cancel.url}")
    private String cancelUrl;
    
    public PayPalService(@Value("${paypal.client.id}") String clientId,
                         @Value("${paypal.client.secret}") String clientSecret,
                         @Value("${paypal.mode}") String mode) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.mode = mode;
        
        PayPalEnvironment environment;
        if ("live".equals(mode)) {
            environment = new PayPalEnvironment.Live(clientId, clientSecret);
        } else {
            environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        }
        this.payPalClient = new PayPalHttpClient(environment);
    }
    
    public Order createAddBalanceOrder(BigDecimal amount, String userId) throws IOException {
        log.info("Creating PayPal order for add balance: {} for user: {}", amount, userId);
        
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        
        ApplicationContext applicationContext = new ApplicationContext()
            .returnUrl(successUrl)
            .cancelUrl(cancelUrl);
        orderRequest.applicationContext(applicationContext);
        
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        purchaseUnits.add(new PurchaseUnitRequest()
            .referenceId("ADDBALANCE")
            .description("Stock Brokerage Account Balance Addition")
            .customId("balance-" + userId + "-" + System.currentTimeMillis())
            .softDescriptor("StockBroker")
            .amountWithBreakdown(new AmountWithBreakdown()
                .currencyCode("USD")
                .value(amount.toString())
            )
        );
        orderRequest.purchaseUnits(purchaseUnits);
        
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.header("prefer", "return=representation");
        request.requestBody(orderRequest);
        
        log.info("Sending PayPal add balance order creation request...");
        try {
            Order result = payPalClient.execute(request).result();
            log.info("PayPal add balance order created successfully: {}", result.id());
            return result;
        } catch (Exception e) {
            log.error("PayPal add balance order creation failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Order captureOrder(String orderId) throws IOException {
        log.info("Capturing PayPal order: {}", orderId);
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.requestBody(new OrderActionRequest());
        
        try {
            Order result = payPalClient.execute(request).result();
            log.info("PayPal order captured successfully: {}", orderId);
            return result;
        } catch (Exception e) {
            log.error("PayPal order capture failed for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }
    
    public Order getOrder(String orderId) throws IOException {
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        return payPalClient.execute(request).result();
    }
}