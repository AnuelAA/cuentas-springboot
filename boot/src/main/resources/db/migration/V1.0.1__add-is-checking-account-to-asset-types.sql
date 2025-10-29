-- ===========================================
-- V1.0.1__add-is-checking-account-to-asset-types.sql
-- ===========================================

-- Agregar campo is_checking_account a la tabla asset_types
ALTER TABLE asset_types 
ADD COLUMN IF NOT EXISTS is_checking_account BOOLEAN NOT NULL DEFAULT FALSE;

-- Actualizar los tipos existentes: solo "Cuenta bancaria" es cuenta corriente
UPDATE asset_types 
SET is_checking_account = TRUE 
WHERE name = 'Cuenta bancaria';

-- Asegurar que los demás tipos tengan is_checking_account = FALSE
UPDATE asset_types 
SET is_checking_account = FALSE 
WHERE name != 'Cuenta bancaria';

