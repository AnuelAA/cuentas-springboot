-- =========================
-- DROP TABLES
-- =========================
DROP TABLE IF EXISTS projections CASCADE;
DROP TABLE IF EXISTS asset_incomes CASCADE;
DROP TABLE IF EXISTS liability_payments CASCADE;
DROP TABLE IF EXISTS liabilities CASCADE;
DROP TABLE IF EXISTS assets CASCADE;
DROP TABLE IF EXISTS d_liability_types CASCADE;
DROP TABLE IF EXISTS d_asset_types CASCADE;
DROP TABLE IF EXISTS records CASCADE;
DROP TABLE IF EXISTS results CASCADE;
DROP TABLE IF EXISTS files CASCADE;
DROP TABLE IF EXISTS categories_types_relations CASCADE;
DROP TABLE IF EXISTS category_types CASCADE;
DROP TABLE IF EXISTS category_relations CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS purchases CASCADE;
DROP TABLE IF EXISTS order_records CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS shippment_cost CASCADE;
DROP TABLE IF EXISTS user_config CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS d_result_types CASCADE;
DROP TABLE IF EXISTS d_record_types CASCADE;
DROP TABLE IF EXISTS d_file_types CASCADE;
DROP TABLE IF EXISTS d_currency CASCADE;

-- =========================
-- TABLAS DE DEFINICIÓN
-- =========================
CREATE TABLE d_currency (
    ID SERIAL PRIMARY KEY,
    currency VARCHAR(255) NOT NULL
);
INSERT INTO d_currency (currency) VALUES ('EUR'), ('USD'), ('GBP'), ('YUAN');

CREATE TABLE d_result_types (
    ID SERIAL PRIMARY KEY,
    result_type VARCHAR(255) NOT NULL
);
INSERT INTO d_result_types (result_type) VALUES
('net'), ('profit'), ('net_income'), ('net_treasury'), ('total_liquid'), ('total_invested');

CREATE TABLE d_record_types (
    ID SERIAL PRIMARY KEY,
    record_type VARCHAR(255) NOT NULL
);
INSERT INTO d_record_types (record_type) VALUES
('income'), ('expense'), ('total_liquid'), ('total_invested'), ('open_investment'), ('close_investment');

CREATE TABLE d_file_types (
    ID SERIAL PRIMARY KEY,
    file_type VARCHAR(255) NOT NULL
);
INSERT INTO d_file_types (file_type) VALUES ('xlsx');

CREATE TABLE category_types (
    ID SERIAL PRIMARY KEY,
    category_type VARCHAR(255) NOT NULL
);
INSERT INTO category_types (category_type) VALUES ('income'), ('expense'), ('liquid'), ('investment');

CREATE TABLE d_asset_types (
    ID SERIAL PRIMARY KEY,
    asset_type VARCHAR(255) NOT NULL
);

CREATE TABLE d_liability_types (
    ID SERIAL PRIMARY KEY,
    liability_type VARCHAR(255) NOT NULL
);

-- =========================
-- USUARIOS Y CONFIGURACIÓN
-- =========================
CREATE TABLE users (
    ID BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    registration_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,
    is_company INT NOT NULL DEFAULT 0
);

CREATE TABLE user_config (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL UNIQUE REFERENCES users(ID),
    dark_mode INT NOT NULL DEFAULT 1
);

-- =========================
-- ARCHIVOS
-- =========================
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT NOT NULL REFERENCES users(id),
    file_type INT NOT NULL REFERENCES d_file_types(id),
    year INT NOT NULL,
    upload_date TIMESTAMP NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    file_data BYTEA  -- binario del Excel
);

-- =========================
-- CATEGORÍAS
-- =========================
CREATE TABLE categories (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    category_name VARCHAR(255) NOT NULL,
    grouped INT NOT NULL DEFAULT 0,
    is_group BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE category_relations (
    ID_cat1 BIGINT NOT NULL REFERENCES categories(ID),
    ID_cat2 BIGINT NOT NULL REFERENCES categories(ID),
    UserID BIGINT NOT NULL REFERENCES users(ID),
    PRIMARY KEY (ID_cat1, ID_cat2)
);

CREATE TABLE categories_types_relations (
    category_id BIGINT NOT NULL REFERENCES categories(ID),
    category_type_id INT NOT NULL REFERENCES category_types(ID),
    user_id BIGINT NOT NULL REFERENCES users(ID),
    PRIMARY KEY (category_id, category_type_id, user_id)
);

-- =========================
-- REGISTROS Y RESULTADOS
-- =========================
CREATE TABLE records (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    type INT NOT NULL REFERENCES d_record_types(ID),
    category BIGINT NOT NULL REFERENCES categories(ID),
    amount DECIMAL(15,2) NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID),
    profit DECIMAL(15,2) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    r_date DATE NOT NULL
);

CREATE TABLE results (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    type INT NOT NULL REFERENCES d_result_types(ID),
    amount DECIMAL(15,2) NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID),
    year INT NOT NULL,
    month INT NOT NULL,
    r_date DATE NOT NULL
);

-- =========================
-- PRODUCTOS, COMPRAS Y PEDIDOS
-- =========================
CREATE TABLE products (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    name VARCHAR(255) NOT NULL,
    SKU VARCHAR(255) NOT NULL,
    UNIQUE (UserID, SKU)
);

CREATE TABLE purchases (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    product BIGINT NOT NULL REFERENCES products(ID),
    quantity INT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID),
    r_date DATE NOT NULL,
    notes VARCHAR(255) NOT NULL
);

CREATE TABLE orders (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    order_id VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    shippment_income INT NOT NULL DEFAULT 0,
    commissions DECIMAL(15,2) NOT NULL,
    shippments INT NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID),
    r_date DATE NOT NULL,
    notes VARCHAR(255) NOT NULL,
    UNIQUE (UserID, order_id)
);

CREATE TABLE order_records (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    order_id BIGINT NOT NULL REFERENCES orders(ID),
    product BIGINT NOT NULL REFERENCES products(ID),
    quantity INT NOT NULL,
    sale_price DECIMAL(15,2) NOT NULL,
    taxes DECIMAL(15,2) NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID)
);

CREATE TABLE shippment_cost (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    amount DECIMAL(15,2) NOT NULL,
    currency INT NOT NULL DEFAULT 1 REFERENCES d_currency(ID),
    month INT NOT NULL,
    year INT NOT NULL,
    UNIQUE (UserID, year, month)
);

-- =========================
-- ACTIVOS, PASIVOS Y RENTAS
-- =========================
CREATE TABLE assets (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    Name VARCHAR(255) NOT NULL,
    AssetType INT NOT NULL REFERENCES d_asset_types(ID),
    Value DECIMAL(15,2) NOT NULL,
    Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE liabilities (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    Name VARCHAR(255) NOT NULL,
    LiabilityType INT NOT NULL REFERENCES d_liability_types(ID),
    Principal DECIMAL(15,2) NOT NULL,
    Interest_rate DECIMAL(5,2),
    Start_date DATE,
    End_date DATE,
    Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE liability_payments (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    LiabilityID BIGINT NOT NULL REFERENCES liabilities(ID),
    Payment_date DATE NOT NULL,
    Amount DECIMAL(15,2) NOT NULL,
    Interest DECIMAL(15,2),
    Principal_paid DECIMAL(15,2)
);

CREATE TABLE asset_incomes (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    AssetID BIGINT NOT NULL REFERENCES assets(ID),
    Income_date DATE NOT NULL,
    Amount DECIMAL(15,2) NOT NULL,
    Description VARCHAR(255)
);

-- =========================
-- PROYECCIONES
-- =========================
CREATE TABLE projections (
    ID BIGSERIAL PRIMARY KEY,
    UserID BIGINT NOT NULL REFERENCES users(ID),
    Name VARCHAR(255) NOT NULL,
    Description TEXT,
    Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
