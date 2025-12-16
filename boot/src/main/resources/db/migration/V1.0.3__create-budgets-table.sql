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
  CONSTRAINT check_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Índice único para prevenir duplicados
-- Nota: PostgreSQL permite múltiples NULLs en índices únicos, así que múltiples presupuestos
-- con start_date NULL para la misma combinación user_id/category_id/period son permitidos
CREATE UNIQUE INDEX unique_budget_per_user_category_period_start 
ON budgets(user_id, category_id, period, start_date);

-- Índice para mejorar consultas por usuario
CREATE INDEX idx_budgets_user_id ON budgets(user_id);

-- Índice para mejorar consultas por categoría
CREATE INDEX idx_budgets_category_id ON budgets(category_id);

-- Índice para mejorar consultas de presupuestos activos por fecha
CREATE INDEX idx_budgets_dates ON budgets(user_id, start_date, end_date);

