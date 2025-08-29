# Stock Brokerage Application - Low Level Design (LLD)

## 1. Database Schema Design

### 1.1 User Management Schema

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACTIVE, SUSPENDED, CLOSED
    kyc_status VARCHAR(20) DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, APPROVED, REJECTED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User KYC table
CREATE TABLE user_kyc (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    document_type VARCHAR(50), -- PASSPORT, DRIVER_LICENSE, SSN
    document_number VARCHAR(100),
    document_url VARCHAR(500),
    verification_status VARCHAR(20), -- PENDING, APPROVED, REJECTED
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User sessions
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    session_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 1.2 Portfolio Management Schema

```sql
-- User wallets
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    currency VARCHAR(3) DEFAULT 'USD',
    balance DECIMAL(15,2) DEFAULT 0.00,
    locked_balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock symbols
CREATE TABLE symbols (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    sector VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User holdings
CREATE TABLE holdings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    symbol_id BIGINT REFERENCES symbols(id),
    quantity BIGINT NOT NULL,
    average_price DECIMAL(10,4) NOT NULL,
    total_investment DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, symbol_id)
);
```

### 1.3 Order Management Schema

```sql
-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    symbol_id BIGINT REFERENCES symbols(id),
    order_type VARCHAR(20) NOT NULL, -- MARKET, LIMIT, STOP_LOSS
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    quantity BIGINT NOT NULL,
    price DECIMAL(10,4), -- NULL for market orders
    stop_price DECIMAL(10,4), -- For stop-loss orders
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, FILLED, PARTIALLY_FILLED, CANCELLED, REJECTED
    filled_quantity BIGINT DEFAULT 0,
    filled_price DECIMAL(10,4),
    order_value DECIMAL(15,2),
    fees DECIMAL(10,2) DEFAULT 0.00,
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Trades table
CREATE TABLE trades (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    symbol_id BIGINT REFERENCES symbols(id),
    side VARCHAR(10) NOT NULL,
    quantity BIGINT NOT NULL,
    price DECIMAL(10,4) NOT NULL,
    trade_value DECIMAL(15,2) NOT NULL,
    fees DECIMAL(10,2) DEFAULT 0.00,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 1.4 Market Data Schema (TimescaleDB)

```sql
-- Market data (time-series)
CREATE TABLE market_data (
    time TIMESTAMPTZ NOT NULL,
    symbol_id BIGINT NOT NULL,
    open_price DECIMAL(10,4),
    high_price DECIMAL(10,4),
    low_price DECIMAL(10,4),
    close_price DECIMAL(10,4),
    volume BIGINT,
    PRIMARY KEY (time, symbol_id)
);

-- Create hypertable for time-series data
SELECT create_hypertable('market_data', 'time');

-- Real-time quotes
CREATE TABLE quotes (
    symbol_id BIGINT PRIMARY KEY,
    bid_price DECIMAL(10,4),
    ask_price DECIMAL(10,4),
    last_price DECIMAL(10,4),
    volume BIGINT,
    change_percent DECIMAL(5,2),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

## 2. API Design

### 2.1 User Management APIs

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        // Implementation
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        // Implementation
    }
    
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfile> getUserProfile(Authentication auth) {
        // Implementation
    }
    
    @PostMapping("/kyc")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<KycSubmissionResponse> submitKyc(
            @Valid @RequestBody KycSubmissionRequest request) {
        // Implementation
    }
}
```

### 2.2 Order Management APIs

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication auth) {
        // Implementation
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Implementation
    }
    
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            Authentication auth) {
        // Implementation
    }
    
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication auth) {
        // Implementation
    }
}
```

### 2.3 Portfolio APIs

```java
@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PortfolioSummary> getPortfolio(Authentication auth) {
        // Implementation
    }
    
    @GetMapping("/holdings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<HoldingResponse>> getHoldings(Authentication auth) {
        // Implementation
    }
    
    @GetMapping("/performance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PerformanceResponse> getPerformance(
            Authentication auth,
            @RequestParam String period) { // 1D, 1W, 1M, 1Y
        // Implementation
    }
}
```

## 3. Service Layer Implementation

### 3.1 Order Service Implementation

```java
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final OrderValidator orderValidator;
    private final OrderExecutor orderExecutor;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public OrderResponse placeOrder(OrderRequest request, Long userId) {
        // 1. Validate user
        User user = userService.getActiveUser(userId);
        
        // 2. Validate order
        orderValidator.validate(request, user);
        
        // 3. Check sufficient funds/holdings
        validateSufficientFundsOrHoldings(request, userId);
        
        // 4. Create order entity
        Order order = createOrderEntity(request, userId);
        
        // 5. Lock funds/holdings
        lockFundsOrHoldings(order);
        
        // 6. Save order
        order = orderRepository.save(order);
        
        // 7. Send to matching engine
        publishOrderEvent(order);
        
        return mapToOrderResponse(order);
    }
    
    private void validateSufficientFundsOrHoldings(OrderRequest request, Long userId) {
        if (request.getSide() == OrderSide.BUY) {
            BigDecimal requiredAmount = calculateRequiredAmount(request);
            portfolioService.validateSufficientFunds(userId, requiredAmount);
        } else {
            portfolioService.validateSufficientHoldings(userId, 
                request.getSymbol(), request.getQuantity());
        }
    }
    
    @EventListener
    public void handleTradeExecution(TradeExecutedEvent event) {
        // Update order status
        // Update portfolio
        // Release locked funds
        // Send notifications
    }
}
```

### 3.2 Portfolio Service Implementation

```java
@Service
@Transactional
public class PortfolioService {
    
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final MarketDataService marketDataService;
    
    public PortfolioSummary getPortfolioSummary(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        
        BigDecimal totalValue = calculateTotalValue(holdings);
        BigDecimal totalPnL = calculateTotalPnL(holdings);
        
        return PortfolioSummary.builder()
            .cashBalance(wallet.getBalance())
            .investedValue(totalValue)
            .totalPnL(totalPnL)
            .holdings(mapToHoldingResponses(holdings))
            .build();
    }
    
    private BigDecimal calculateTotalValue(List<Holding> holdings) {
        return holdings.stream()
            .map(this::calculateHoldingValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateHoldingValue(Holding holding) {
        BigDecimal currentPrice = marketDataService.getCurrentPrice(holding.getSymbolId());
        return currentPrice.multiply(new BigDecimal(holding.getQuantity()));
    }
    
    @Transactional
    public void updateHolding(Long userId, Long symbolId, Long quantity, 
                             BigDecimal price, OrderSide side) {
        Holding holding = holdingRepository.findByUserIdAndSymbolId(userId, symbolId)
            .orElse(new Holding(userId, symbolId));
            
        if (side == OrderSide.BUY) {
            // Update average price and quantity
            BigDecimal totalCost = holding.getTotalInvestment()
                .add(price.multiply(new BigDecimal(quantity)));
            Long totalQuantity = holding.getQuantity() + quantity;
            
            holding.setQuantity(totalQuantity);
            holding.setAveragePrice(totalCost.divide(new BigDecimal(totalQuantity), 4, RoundingMode.HALF_UP));
            holding.setTotalInvestment(totalCost);
        } else {
            // Reduce quantity
            holding.setQuantity(holding.getQuantity() - quantity);
            if (holding.getQuantity() == 0) {
                holdingRepository.delete(holding);
                return;
            }
        }
        
        holdingRepository.save(holding);
    }
}
```

## 4. WebSocket Implementation for Real-time Updates

```java
@Component
public class MarketDataWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        
        // Send initial portfolio data
        sendPortfolioUpdate(userId, session);
    }
    
    @KafkaListener(topics = "market-data-updates")
    public void handleMarketDataUpdate(MarketDataUpdate update) {
        // Broadcast to all connected sessions
        sessions.values().parallelStream()
            .forEach(session -> sendMarketDataUpdate(session, update));
    }
    
    @KafkaListener(topics = "order-updates")
    public void handleOrderUpdate(OrderUpdate update) {
        WebSocketSession session = sessions.get(update.getUserId().toString());
        if (session != null && session.isOpen()) {
            sendOrderUpdate(session, update);
        }
    }
    
    private void sendMarketDataUpdate(WebSocketSession session, MarketDataUpdate update) {
        try {
            String message = objectMapper.writeValueAsString(update);
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            log.error("Error sending market data update", e);
        }
    }
}
```

## 5. Security Implementation

### 5.1 JWT Authentication

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) {
        String token = extractToken(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, 
                    userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 5.2 Rate Limiting

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientId = extractClientId(httpRequest);
        String endpoint = httpRequest.getRequestURI();
        
        String key = "rate_limit:" + clientId + ":" + endpoint;
        String count = redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
        } else if (Integer.parseInt(count) >= getRateLimit(endpoint)) {
            ((HttpServletResponse) response).setStatus(429);
            return;
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        chain.doFilter(request, response);
    }
}
```

## 6. Configuration Classes

### 6.1 Database Configuration

```java
@Configuration
@EnableJpaRepositories
public class DatabaseConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.timeseries")
    public DataSource timeseriesDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setHostName("localhost");
        factory.setPort(6379);
        return factory;
    }
}
```

### 6.2 Kafka Configuration

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stock-brokerage");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```