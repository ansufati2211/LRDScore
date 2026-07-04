-- MÓDULO 6: Disponibilidad de Productos (Doble "86")

-- R6-1/R6-2: estado de disponibilidad por producto
--   DISPONIBLE       → estado por defecto, visible en carta
--   AGOTADO_TEMPORAL → marcado desde KDS, reversible desde cocina (E6-2)
--   AGOTADO_SERVICIO → marcado desde KDS, solo se revierte en cierre de caja (R6-3/E6-3)
ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS estado_disponibilidad VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE';
