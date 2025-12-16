-- ===========================================
-- V1.0.4__create-transaction-templates-table.sql
-- ===========================================

CREATE TABLE transaction_templates (
  template_id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  category_id INTEGER REFERENCES categories(category_id) ON DELETE SET NULL,
  category_name VARCHAR(255),
  type VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense')),
  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
  asset_id INTEGER REFERENCES assets(asset_id) ON DELETE SET NULL,
  related_asset_id INTEGER REFERENCES assets(asset_id) ON DELETE SET NULL,
  liability_id INTEGER REFERENCES liabilities(liability_id) ON DELETE SET NULL,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

-- Índice para mejorar consultas por usuario
CREATE INDEX idx_transaction_templates_user_id ON transaction_templates(user_id);

-- Índice para mejorar consultas por tipo
CREATE INDEX idx_transaction_templates_type ON transaction_templates(user_id, type);

