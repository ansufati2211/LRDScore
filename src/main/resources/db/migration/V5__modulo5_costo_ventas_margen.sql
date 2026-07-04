-- MÓDULO 5: Costo de Ventas y Margen

-- 1. Snapshot de costo promedio ponderado al momento del consumo (R5-1)
--    Se congela al pasar a EN_PREPARACION; jamás se recalcula después.
ALTER TABLE pedidos_detalle
    ADD COLUMN IF NOT EXISTS costo_unitario_consumido NUMERIC(12, 4);

-- 2. Costo referencial configurable para productos sin receta (R5-4)
--    Aplica cuando es_preparado = FALSE (ej. bebidas envasadas, postres externos).
ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS costo_referencial NUMERIC(12, 4);
