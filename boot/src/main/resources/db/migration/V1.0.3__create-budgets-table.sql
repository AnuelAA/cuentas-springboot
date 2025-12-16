-- ===========================================
-- V1.0.3__create-budgets-table.sql
-- ===========================================

CREATE TABLE budgets (
  budget_id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  category_id INTEGER NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
  period VARCHAR(10) NOT NULL CHECK (period IN ('monthly', 'yearly')),
  start_date DATE,
  end_date DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT check_end_after_start CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT unique_budget_per_user_category_period_start UNIQUE(user_id, category_id, period, COALESCE(start_date, DATE '1900-01-01'))
);

-- Índice para mejorar consultas por usuario
CREATE INDEX idx_budgets_user_id ON budgets(user_id);

-- Índice para mejorar consultas por categoría
CREATE INDEX idx_budgets_category_id ON budgets(category_id);

-- Índice para mejorar consultas de presupuestos activos por fecha
CREATE INDEX idx_budgets_dates ON budgets(user_id, start_date, end_date);

