ALTER TABLE kardex_movimientos ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- 1. Le asignamos una suscripción activa con el Plan Completo (plan_id = 2) a la Empresa 1
INSERT INTO suscripciones (empresa_id, plan_id, estado, fecha_inicio, fecha_fin, created_at, updated_at)
VALUES (1, 2, 'ACTIVA', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 year', NOW(), NOW());

-- 2. Le decimos a la Empresa 1 cuál es su suscripción vigente
UPDATE empresas 
SET suscripcion_vigente_id = (SELECT id FROM suscripciones WHERE empresa_id = 1 ORDER BY id DESC LIMIT 1) 
WHERE id = 1;

-- 1. Borramos la regla antigua
ALTER TABLE kardex_movimientos DROP CONSTRAINT IF EXISTS kardex_movimientos_tipo_movimiento_check;

-- 2. Creamos la regla nueva incluyendo RESERVA, LIBERACION_RESERVA y CONSUMO_PRODUCCION
ALTER TABLE kardex_movimientos ADD CONSTRAINT kardex_movimientos_tipo_movimiento_check 
CHECK (tipo_movimiento IN (
    'INICIAL', 
    'ENTRADA_COMPRA', 
    'ENTRADA_AJUSTE', 
    'SALIDA_MERMA', 
    'SALIDA_AJUSTE', 
    'SALIDA_PRODUCCION', 
    'RESERVA', 
    'LIBERACION_RESERVA', 
    'CONSUMO_PRODUCCION'
));

DROP VIEW IF EXISTS vw_kds_cocina;

CREATE VIEW vw_kds_cocina AS
SELECT
    pd.id                                                   AS detalle_id,
    pd.empresa_id,
    p.sede_id,
    p.id                                                    AS pedido_id,
    p.numero_orden,
    p.tipo_consumo,
    p.identificador_mesa_referencia                         AS mesa,
    p.estado_actual                                         AS estado_pedido,
    p.notas_generales,
    -- 🔥 FIX 1: Le decimos explícitamente que la hora almacenada es de Perú
    (p.created_at AT TIME ZONE 'America/Lima')::TIMESTAMPTZ AS hora_ingreso,
    -- 🔥 FIX 2: Calculamos la diferencia comparando peras con peras (Tiempos con Zona Horaria)
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (p.created_at AT TIME ZONE 'America/Lima'))) / 60.0 AS minutos_transcurridos,
    pd.cantidad,
    pd.notas_preparacion,
    pr.id                                                   AS producto_id,
    pr.nombre                                               AS producto,
    pr.tiempo_preparacion_minutos,
    pd.estado_item,
    pd.numero_comanda
FROM pedidos_detalle pd
JOIN pedidos p ON pd.pedido_id = p.id
JOIN productos pr ON pd.producto_id = pr.id
WHERE p.estado_actual IN ('RECIBIDO', 'EN_PREPARACION')
  AND pd.estado_item IN ('PENDIENTE', 'EN_PREPARACION');