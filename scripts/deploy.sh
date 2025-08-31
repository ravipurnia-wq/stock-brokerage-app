#!/bin/bash

# Stock Brokerage Application Deployment Script
# This script builds and deploys the application using Docker Compose

set -e

echo "🚀 Starting Stock Brokerage Application Deployment..."

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install it and try again."
    exit 1
fi

# Set environment
ENVIRONMENT=${1:-development}
print_status "Deploying in $ENVIRONMENT environment"

# Load environment variables
if [ "$ENVIRONMENT" = "production" ]; then
    if [ -f ".env.production" ]; then
        print_status "Loading production environment variables..."
        export $(cat .env.production | grep -v '#' | xargs)
    else
        print_warning "No .env.production file found. Using default values."
        print_warning "Please create .env.production from .env.production.example"
    fi
else
    if [ -f ".env" ]; then
        print_status "Loading development environment variables..."
        export $(cat .env | grep -v '#' | xargs)
    else
        print_warning "No .env file found. Using default values."
    fi
fi

# Create necessary directories
print_status "Creating necessary directories..."
mkdir -p docker/nginx/ssl
mkdir -p docker/mongodb/init-scripts
mkdir -p logs

# Stop existing containers
print_status "Stopping existing containers..."
docker-compose down --remove-orphans

# Pull latest images
print_status "Pulling latest images..."
docker-compose pull

# Build the application
print_status "Building the Stock Brokerage application..."
docker-compose build --no-cache stock-brokerage-app

# Start the services
print_status "Starting services..."
if [ "$ENVIRONMENT" = "production" ]; then
    docker-compose up -d
else
    docker-compose up -d
fi

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 30

# Check service health
print_status "Checking service health..."

# Check MongoDB
if docker-compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
    print_success "MongoDB is healthy"
else
    print_warning "MongoDB health check failed"
fi

# Check Redis
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    print_success "Redis is healthy"
else
    print_warning "Redis health check failed"
fi

# Check Kafka
if docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
    print_success "Kafka is healthy"
else
    print_warning "Kafka health check failed"
fi

# Check Application
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_success "Stock Brokerage Application is healthy"
else
    print_warning "Application health check failed"
fi

# Check Nginx
if curl -f http://localhost/ > /dev/null 2>&1; then
    print_success "Nginx is healthy"
else
    print_warning "Nginx health check failed"
fi

print_success "🎉 Deployment completed!"
print_status "Services are available at:"
print_status "  • Application: http://localhost/"
print_status "  • API Health: http://localhost/health"
print_status "  • MongoDB: localhost:27017"
print_status "  • Redis: localhost:6379"
print_status "  • Kafka: localhost:9092"

print_status "To view logs: docker-compose logs -f"
print_status "To stop services: docker-compose down"