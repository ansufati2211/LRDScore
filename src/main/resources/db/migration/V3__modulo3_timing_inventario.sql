-- =============================================================================
-- MÓDULO 3: Timing de Inventario — Reserva / Consumo / Liberación / Merma
-- =============================================================================

-- 1. Flag de revisión en pedidos (E3-1: stock insuficiente al iniciar preparación)
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS requiere_revision BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Stock reservado acumulado en insumos (gestionado por la capa de servicio)
--    stock_disponible = stock_actual - stock_reservado (propiedad derivada, no almacenada)
ALTER TABLE insumos ADD COLUMN IF NOT EXISTS stock_reservado NUMERIC(12, 3) NOT NULL DEFAULT 0;
