-- ============================================================
-- Migración 001 · C-26 Movimiento simulado del mercado
-- Para bases de datos creadas con el script v1.1 ANTERIOR a este cambio.
-- No borra datos: añade la fecha de última actualización de precio a cada activo.
-- (Si recreas la base con script-base-datos.sql no hace falta ejecutarla.)
-- ============================================================
USE eduwealth_db;

ALTER TABLE activos
    ADD COLUMN fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER id_divisa;
