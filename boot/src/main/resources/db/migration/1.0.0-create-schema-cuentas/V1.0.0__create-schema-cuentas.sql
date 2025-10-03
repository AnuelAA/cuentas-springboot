-- ===========================================
-- V1.0.1__create-schema-cuentas.sql
-- ===========================================

-- ===============================
-- 0. DROP TABLES
-- ===============================
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS liabilities;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS asset_types;
DROP TABLE IF EXISTS liability_types;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS user_settings;
DROP TABLE IF EXISTS users;

-- ===============================
-- 1. Usuarios
-- ===============================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE user_settings (
    setting_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    dark_mode BOOLEAN DEFAULT TRUE,
    language VARCHAR(50),
    notifications_email BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ===============================
-- 2. Categorías y Tipologías
-- ===============================
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE asset_types (
    asset_type_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE liability_types (
    liability_type_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- ===============================
-- 3. Activos y Pasivos
-- ===============================
CREATE TABLE assets (
    asset_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    asset_type_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    acquisition_date DATE,
    acquisition_value DECIMAL(15,2),
    current_value DECIMAL(15,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_assets_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_assets_type FOREIGN KEY (asset_type_id) REFERENCES asset_types(asset_type_id) ON DELETE RESTRICT
);

CREATE TABLE liabilities (
    liability_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    liability_type_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    principal_amount DECIMAL(15,2),
    interest_rate DECIMAL(15,5),
    start_date DATE,
    end_date DATE,
    outstanding_balance DECIMAL(15,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_liabilities_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_liabilities_type FOREIGN KEY (liability_type_id) REFERENCES liability_types(liability_type_id) ON DELETE RESTRICT
);

-- ===============================
-- 4. Transacciones
-- ===============================
CREATE TABLE transactions (
    transaction_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT,
    asset_id INT,
    related_asset_id INT,
    liability_id INT,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('income','expense','neutral')),
    amount DECIMAL(15,2) NOT NULL,
    transaction_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    CONSTRAINT fk_transactions_asset FOREIGN KEY (asset_id) REFERENCES assets(asset_id) ON DELETE SET NULL,
    CONSTRAINT fk_transactions_liability FOREIGN KEY (liability_id) REFERENCES liabilities(liability_id) ON DELETE SET NULL
);

-- ===============================
-- 5. Inserts iniciales de definición
-- ===============================

INSERT INTO asset_types (name, description) VALUES
('Inmueble','Inmueble'),
('Fondo de inversión','Fondo de inversión'),
('Cuenta bancaria','Cuenta bancaria');

INSERT INTO liability_types (name, description) VALUES
('Hipoteca','Hipoteca'),
('Préstamo personal','Préstamo personal');
