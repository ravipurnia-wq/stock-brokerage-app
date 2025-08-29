# Stock Brokerage Application

A complete stock brokerage application built with Spring Boot, MongoDB, Kafka, and Redis.

## Features

- **User Management**: Registration, authentication, KYC
- **Order Management**: Place, cancel, and track orders (Market, Limit, Stop-Loss)
- **Portfolio Management**: Track holdings, P&L, and performance
- **Market Data**: Real-time stock prices with Kafka streaming
- **Payment System**: Deposit, withdraw, and transaction history
- **WebSocket**: Real-time updates for market data and portfolio
- **Security**: JWT authentication and role-based access control

## Tech Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MongoDB
- **Message Broker**: Apache Kafka
- **Cache**: Redis
- **Security**: JWT, Spring Security
- **WebSocket**: Spring WebSocket with SockJS

## Prerequisites

- Java 17+
- MongoDB running on localhost:27017
- Apache Kafka running on localhost:9092
- Redis running on localhost:6379 (optional, for caching)

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/me` - Get current user

### Orders
- `POST /api/v1/orders` - Place order
- `GET /api/v1/orders` - Get user orders
- `GET /api/v1/orders/{id}` - Get order by ID
- `DELETE /api/v1/orders/{id}` - Cancel order

### Portfolio
- `GET /api/v1/portfolio` - Get portfolio summary
- `GET /api/v1/portfolio/holdings` - Get holdings

### Payments
- `POST /api/v1/payments/deposit` - Deposit funds
- `POST /api/v1/payments/withdraw` - Withdraw funds
- `GET /api/v1/payments/transactions` - Get transaction history

### Market Data
- `GET /api/v1/market/symbols` - Get all symbols
- `GET /api/v1/market/data/{symbolId}` - Get market data
- `POST /api/v1/market/initialize` - Initialize sample data

### WebSocket
- `ws://localhost:8080/ws/market-data` - Real-time market data stream

## Running the Application

1. Start MongoDB, Kafka, and Redis services
2. Build the application:
   ```bash
   mvn clean package
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## Sample API Usage

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

### 3. Place an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "symbol": "AAPL",
    "orderType": "MARKET",
    "side": "BUY",
    "quantity": 10
  }'
```

## Kafka Topics

The application uses the following Kafka topics:
- `market-data` - Real-time market data updates
- `order-events` - Order placement events
- `trade-events` - Trade execution events
- `portfolio-updates` - Portfolio change events

## WebSocket Events

Connect to `/ws/market-data` to receive:
- Market data updates
- Portfolio updates
- Order status changes

## Architecture

The application follows a microservices-inspired architecture with:
- Service layer for business logic
- Repository layer for data access
- Event-driven communication via Kafka
- Real-time updates via WebSocket
- Caching with Redis
- JWT-based security

## Development Notes

- The application initializes with sample stock symbols (AAPL, GOOGL, MSFT, TSLA, AMZN)
- Market data is simulated with random price changes every 30 seconds
- All monetary values use BigDecimal for precision
- Comprehensive error handling and logging
- RESTful API design with proper HTTP status codes