-- ============================================================
-- Migración 002 · C-28 Capital inicial del simulador por usuario
-- Para bases creadas antes de este cambio. No borra datos: los usuarios existentes
-- empezaron con 10.000 en su divisa base, que es el valor por defecto.
-- (Si recreas la base con script-base-datos.sql no hace falta ejecutarla.)
-- ============================================================
USE eduwealth_db;

ALTER TABLE usuarios
    ADD COLUMN capital_inicial DECIMAL(15,2) NOT NULL DEFAULT 10000.00 AFTER saldo_ficticio,
    ADD CONSTRAINT chk_usuario_capital CHECK (capital_inicial > 0);
