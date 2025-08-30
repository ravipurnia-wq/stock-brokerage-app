package com.stockbrokerage.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class WebController {

    @Value("${paypal.client.id}")
    private String paypalClientId;

    @GetMapping("/")
    public String home() {
        return "redirect:/trading";
    }

    @GetMapping("/trading")
    public String tradingPlatform() {
        return "redirect:/stock-trading-platform.html";
    }

    @GetMapping("/stock-trading-platform.html")
    public ResponseEntity<String> stockTradingPlatformWithPayPal() throws IOException {
        // Read the HTML file
        ClassPathResource resource = new ClassPathResource("static/stock-trading-platform.html");
        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        
        // Replace the PayPal client ID placeholder
        content = content.replace("CLIENT_ID_PLACEHOLDER", paypalClientId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    @GetMapping("/websocket-test")
    public String websocketTest() {
        return "websocket-test.html";
    }

    @GetMapping("/test")
    public String test() {
        return "websocket-test.html";
    }
}