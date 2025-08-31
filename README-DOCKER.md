# Docker Deployment Guide for Stock Brokerage Application

This guide explains how to deploy the Stock Brokerage Application using Docker and Docker Compose.

## 🏗️ Architecture

The application uses a microservices architecture with the following components:

- **Spring Boot Application** - Main trading platform
- **MongoDB** - Primary database for user data, orders, and portfolios
- **Redis** - Caching and session management
- **Apache Kafka** - Event streaming for real-time updates
- **Nginx** - Reverse proxy and load balancer

## 📋 Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available for containers
- Ports 80, 443, 6379, 9092, 27017 available

## 🚀 Quick Start

### 1. Clone and Setup

```bash
git clone https://github.com/ravipurnia-wq/stock-brokerage-app.git
cd stock-brokerage-app
```

### 2. Environment Configuration

Copy the environment template:
```bash
cp .env.example .env
```

Edit `.env` with your configuration:
```bash
# PayPal Configuration
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYPAL_MODE=sandbox

# Finnhub API
FINNHUB_API_TOKEN=your_finnhub_token

# Security
JWT_SECRET=your_secure_jwt_secret_minimum_32_characters
MONGODB_ROOT_PASSWORD=secure_mongodb_password
REDIS_PASSWORD=secure_redis_password
```

### 3. Deploy with Script

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

### 4. Manual Deployment

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f stock-brokerage-app
```

## 🌐 Access Points

Once deployed, access the application at:

- **Main Application**: http://localhost/
- **API Health Check**: http://localhost/health
- **Trading Platform**: http://localhost/trading

## 📊 Service Endpoints

| Service | Internal Port | External Port | Health Check |
|---------|---------------|---------------|--------------|
| Nginx | 80, 443 | 80, 443 | http://localhost/ |
| Spring Boot | 8080 | - | http://localhost/health |
| MongoDB | 27017 | 27017 | mongosh connection |
| Redis | 6379 | 6379 | redis-cli ping |
| Kafka | 9092 | 9092 | kafka-topics --list |

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PAYPAL_CLIENT_ID` | PayPal client ID | - | Yes |
| `PAYPAL_CLIENT_SECRET` | PayPal client secret | - | Yes |
| `PAYPAL_MODE` | PayPal mode (sandbox/live) | sandbox | No |
| `FINNHUB_API_TOKEN` | Finnhub API token | - | Yes |
| `JWT_SECRET` | JWT signing secret | - | Yes |
| `MONGODB_ROOT_PASSWORD` | MongoDB root password | stockbrokerage123 | No |
| `REDIS_PASSWORD` | Redis password | redis123 | No |

### Volume Mounts

The following volumes persist data:

- `mongodb_data` - MongoDB database files
- `redis_data` - Redis persistence files
- `kafka_data` - Kafka logs and topics
- `app_logs` - Application log files
- `nginx_logs` - Nginx access and error logs

## 🔒 Security Configuration

### Nginx Security Headers

The Nginx configuration includes:
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Content Security Policy
- Rate limiting for API endpoints

### Database Security

- MongoDB with authentication enabled
- Redis with password protection
- Network isolation using Docker networks

### Application Security

- JWT-based authentication
- Secure WebSocket connections
- Environment-based configuration

## 📈 Monitoring and Health Checks

### Health Checks

All services include health checks:

```bash
# Check all services
docker-compose ps

# Individual service health
docker-compose exec stock-brokerage-app curl http://localhost:8080/actuator/health
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"
docker-compose exec redis redis-cli ping
```

### Logs

View logs for troubleshooting:

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f stock-brokerage-app
docker-compose logs -f nginx
docker-compose logs -f mongodb
```

## 🔄 Management Commands

### Start Services

```bash
docker-compose up -d
```

### Stop Services

```bash
docker-compose down
```

### Restart Services

```bash
docker-compose restart
```

### Update Application

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose down
docker-compose build --no-cache stock-brokerage-app
docker-compose up -d
```

### Scale Services

```bash
# Scale application instances
docker-compose up -d --scale stock-brokerage-app=3
```

### Backup Data

```bash
# MongoDB backup
docker-compose exec mongodb mongodump --out /data/backup

# Redis backup
docker-compose exec redis redis-cli BGSAVE
```

## 🐛 Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check port usage
   netstat -tulpn | grep :80
   
   # Change ports in docker-compose.yml if needed
   ```

2. **Memory Issues**
   ```bash
   # Check container memory usage
   docker stats
   
   # Increase Docker memory limit if needed
   ```

3. **Database Connection Issues**
   ```bash
   # Check MongoDB logs
   docker-compose logs mongodb
   
   # Test connection
   docker-compose exec mongodb mongosh
   ```

4. **Application Not Starting**
   ```bash
   # Check application logs
   docker-compose logs stock-brokerage-app
   
   # Verify environment variables
   docker-compose exec stock-brokerage-app env | grep -E "(PAYPAL|FINNHUB|JWT)"
   ```

### Performance Tuning

1. **JVM Settings**
   Edit `docker-compose.yml` to adjust JVM memory:
   ```yaml
   environment:
     JAVA_OPTS: "-Xmx1g -Xms512m"
   ```

2. **Database Settings**
   Adjust MongoDB memory settings in `docker-compose.yml`

3. **Nginx Caching**
   Modify nginx configuration for static file caching

## 🚀 Production Deployment

For production deployment:

1. Copy production environment:
   ```bash
   cp .env.production.example .env.production
   ```

2. Configure SSL certificates in `docker/nginx/ssl/`

3. Update nginx configuration for HTTPS

4. Deploy with production profile:
   ```bash
   ./scripts/deploy.sh production
   ```

## 🔧 Development Mode

For development with hot reload:

```bash
# Start dependencies only
docker-compose up -d mongodb redis kafka nginx

# Run application locally
mvn spring-boot:run
```

## 📝 Additional Resources

- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Nginx Configuration Reference](https://nginx.org/en/docs/)
- [MongoDB Docker Guide](https://hub.docker.com/_/mongo)
- [Redis Docker Guide](https://hub.docker.com/_/redis)