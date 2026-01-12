-- EagleBank Database Schema
-- MySQL 8.0+ compatible

-- Create database (uncomment if needed)
CREATE DATABASE IF NOT EXISTS eaglebank CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eaglebank;

-- Drop tables in reverse order of dependencies (for clean recreation)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS bank_accounts;
DROP TABLE IF EXISTS users;

-- ============================================
-- Users Table
-- ============================================
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255),
    
    -- Embedded Address fields
    address_line1 VARCHAR(100),
    address_line2 VARCHAR(100),
    address_line3 VARCHAR(100),
    address_town VARCHAR(100),
    address_county VARCHAR(50),
    address_postcode VARCHAR(20),
    
    phone_number VARCHAR(20),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_created_timestamp (created_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bank Accounts Table
-- ============================================
CREATE TABLE bank_accounts (
    account_number VARCHAR(8) PRIMARY KEY,
    sort_code VARCHAR(8) NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    INDEX idx_bank_accounts_user_id (user_id),
    INDEX idx_bank_accounts_sort_code (sort_code),
    INDEX idx_bank_accounts_created_timestamp (created_timestamp),
    
    -- Ensure balance is non-negative
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    
    -- Ensure account number matches pattern (01xxxxxx)
    CONSTRAINT chk_account_number_pattern CHECK (account_number REGEXP '^01[0-9]{6}$'),
    
    -- Ensure account_type is valid
    CONSTRAINT chk_account_type CHECK (account_type IN ('personal')),
    
    -- Ensure currency is valid
    CONSTRAINT chk_currency CHECK (currency IN ('GBP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Transactions Table
-- ============================================
CREATE TABLE transactions (
    id VARCHAR(50) PRIMARY KEY,
    account_number VARCHAR(8) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    reference VARCHAR(255),
    user_id VARCHAR(50) NOT NULL,
    
    created_timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (account_number) REFERENCES bank_accounts(account_number) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    INDEX idx_transactions_account_number (account_number),
    INDEX idx_transactions_user_id (user_id),
    INDEX idx_transactions_type (type),
    INDEX idx_transactions_created_timestamp (created_timestamp),
    INDEX idx_transactions_account_created (account_number, created_timestamp),
    
    -- Ensure amount is positive
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    
    -- Ensure amount is within limits (0.00 to 10000.00)
    CONSTRAINT chk_amount_range CHECK (amount >= 0.00 AND amount <= 10000.00),
    
    -- Ensure transaction type is valid
    CONSTRAINT chk_transaction_type CHECK (type IN ('deposit', 'withdrawal')),
    
    -- Ensure currency is valid
    CONSTRAINT chk_transaction_currency CHECK (currency IN ('GBP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Comments for documentation
-- ============================================
ALTER TABLE users COMMENT = 'Stores user information with embedded address';
ALTER TABLE bank_accounts COMMENT = 'Stores bank account information linked to users';
ALTER TABLE transactions COMMENT = 'Stores transaction history. Transactions are immutable once created.';
