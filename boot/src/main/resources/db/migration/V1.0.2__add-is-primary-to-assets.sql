-- ===========================================
-- V1.0.2__add-is-primary-to-assets.sql
-- ===========================================

-- Agregar campo is_primary a la tabla assets
ALTER TABLE assets 
ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

-- Crear índice para mejorar las consultas de activo principal
CREATE INDEX IF NOT EXISTS idx_assets_user_primary ON assets(user_id, is_primary) WHERE is_primary = TRUE;

