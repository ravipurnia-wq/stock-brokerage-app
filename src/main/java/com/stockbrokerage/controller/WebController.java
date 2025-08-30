package com.stockbrokerage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "redirect:/trading";
    }

    @GetMapping("/trading")
    public String tradingPlatform() {
        return "stock-trading-platform.html";
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