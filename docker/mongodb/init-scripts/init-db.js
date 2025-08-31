// MongoDB Initialization Script for Stock Brokerage Application

// Create application user with read/write permissions
db = db.getSiblingDB('stock_brokerage');

// Create collections with validation rules
db.createCollection('users', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['email', 'firstName', 'lastName', 'password'],
      properties: {
        email: {
          bsonType: 'string',
          pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
          description: 'must be a valid email address'
        },
        firstName: {
          bsonType: 'string',
          minLength: 1,
          maxLength: 50,
          description: 'must be a string between 1-50 characters'
        },
        lastName: {
          bsonType: 'string',
          minLength: 1,
          maxLength: 50,
          description: 'must be a string between 1-50 characters'
        },
        password: {
          bsonType: 'string',
          minLength: 6,
          description: 'must be a string with minimum 6 characters'
        },
        status: {
          enum: ['ACTIVE', 'INACTIVE', 'SUSPENDED'],
          description: 'must be one of the enum values'
        },
        kycStatus: {
          enum: ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'],
          description: 'must be one of the enum values'
        }
      }
    }
  }
});

db.createCollection('symbols', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['symbol', 'companyName', 'exchange'],
      properties: {
        symbol: {
          bsonType: 'string',
          pattern: '^[A-Z]+$',
          maxLength: 10,
          description: 'must be uppercase letters only, max 10 characters'
        },
        companyName: {
          bsonType: 'string',
          minLength: 1,
          maxLength: 200,
          description: 'must be a string between 1-200 characters'
        },
        exchange: {
          bsonType: 'string',
          minLength: 1,
          maxLength: 10,
          description: 'must be a string between 1-10 characters'
        }
      }
    }
  }
});

db.createCollection('orders', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['userId', 'symbolId', 'orderType', 'side', 'quantity'],
      properties: {
        orderType: {
          enum: ['MARKET', 'LIMIT'],
          description: 'must be either MARKET or LIMIT'
        },
        side: {
          enum: ['BUY', 'SELL'],
          description: 'must be either BUY or SELL'
        },
        status: {
          enum: ['PENDING', 'FILLED', 'CANCELLED', 'REJECTED'],
          description: 'must be one of the enum values'
        },
        quantity: {
          bsonType: 'int',
          minimum: 1,
          description: 'must be a positive integer'
        }
      }
    }
  }
});

// Create indexes for better performance
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ status: 1 });
db.users.createIndex({ createdAt: 1 });

db.symbols.createIndex({ symbol: 1 }, { unique: true });
db.symbols.createIndex({ exchange: 1 });
db.symbols.createIndex({ active: 1 });

db.orders.createIndex({ userId: 1 });
db.orders.createIndex({ symbolId: 1 });
db.orders.createIndex({ status: 1 });
db.orders.createIndex({ createdAt: 1 });
db.orders.createIndex({ userId: 1, status: 1 });

db.holdings.createIndex({ userId: 1 });
db.holdings.createIndex({ symbolId: 1 });
db.holdings.createIndex({ userId: 1, symbolId: 1 }, { unique: true });

db.wallets.createIndex({ userId: 1 }, { unique: true });

db.transactions.createIndex({ userId: 1 });
db.transactions.createIndex({ type: 1 });
db.transactions.createIndex({ status: 1 });
db.transactions.createIndex({ createdAt: 1 });
db.transactions.createIndex({ userId: 1, createdAt: 1 });

db.userWatchlists.createIndex({ userId: 1 });
db.userWatchlists.createIndex({ symbolId: 1 });
db.userWatchlists.createIndex({ userId: 1, symbolId: 1 }, { unique: true });

// Insert some initial data
print('Inserting initial symbols...');

db.symbols.insertMany([
  {
    symbol: 'AAPL',
    companyName: 'Apple Inc.',
    exchange: 'NASDAQ',
    sector: 'Technology',
    active: true,
    createdAt: new Date()
  },
  {
    symbol: 'GOOGL',
    companyName: 'Alphabet Inc.',
    exchange: 'NASDAQ',
    sector: 'Technology',
    active: true,
    createdAt: new Date()
  },
  {
    symbol: 'MSFT',
    companyName: 'Microsoft Corporation',
    exchange: 'NASDAQ',
    sector: 'Technology',
    active: true,
    createdAt: new Date()
  },
  {
    symbol: 'TSLA',
    companyName: 'Tesla, Inc.',
    exchange: 'NASDAQ',
    sector: 'Automotive',
    active: true,
    createdAt: new Date()
  },
  {
    symbol: 'AMZN',
    companyName: 'Amazon.com, Inc.',
    exchange: 'NASDAQ',
    sector: 'E-commerce',
    active: true,
    createdAt: new Date()
  }
]);

print('Database initialization completed successfully!');
print('Created collections: users, symbols, orders, holdings, wallets, transactions, userWatchlists');
print('Created indexes for optimal performance');
print('Inserted initial stock symbols: AAPL, GOOGL, MSFT, TSLA, AMZN');