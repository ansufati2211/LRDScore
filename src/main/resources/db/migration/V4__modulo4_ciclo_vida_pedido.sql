-- MÓDULO 4: Ciclo de vida del pedido — adición / split / cancelación granular

-- 1. Campos en pedidos_detalle
ALTER TABLE pedidos_detalle
    ADD COLUMN IF NOT EXISTS estado_item       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN IF NOT EXISTS numero_comanda    INTEGER     NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS motivo_cancelacion TEXT;

-- 2. Sincronizar estado_item de ítems existentes con el estado actual del pedido
UPDATE pedidos_detalle pd
SET estado_item = CASE
    WHEN p.estado_actual = 'EN_PREPARACION' THEN 'EN_PREPARACION'
    WHEN p.estado_actual = 'LISTO'          THEN 'LISTO'
    WHEN p.estado_actual = 'ENTREGADO'      THEN 'ENTREGADO'
    WHEN p.estado_actual = 'PAGADO'         THEN 'ENTREGADO'
    WHEN p.estado_actual = 'CANCELADO'      THEN 'CANCELADO'
    ELSE 'PENDIENTE'
END
FROM pedidos p
WHERE pd.pedido_id = p.id;

-- 3. Tabla de documentos de cobro (split billing)
CREATE TABLE IF NOT EXISTS documentos_cobro (
    id         BIGSERIAL     PRIMARY KEY,
    empresa_id BIGINT        NOT NULL,
    pedido_id  BIGINT        NOT NULL REFERENCES pedidos(id),
    tipo       VARCHAR(10)   NOT NULL DEFAULT 'ITEMS',    -- ITEMS | MONTO
    monto      NUMERIC(10,2),                              -- solo para tipo MONTO
    subtotal   NUMERIC(10,2) NOT NULL DEFAULT 0,
    total      NUMERIC(10,2) NOT NULL DEFAULT 0,
    estado     VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | PAGADO
    created_at TIMESTAMP     DEFAULT NOW(),
    updated_at TIMESTAMP     DEFAULT NOW()
);

-- 4. Relación N:M entre documento de cobro e ítems del pedido (split por ítem)
CREATE TABLE IF NOT EXISTS documentos_cobro_detalle (
    documento_cobro_id BIGINT NOT NULL REFERENCES documentos_cobro(id),
    pedido_detalle_id  BIGINT NOT NULL REFERENCES pedidos_detalle(id),
    PRIMARY KEY (documento_cobro_id, pedido_detalle_id)
);

-- 5. Vista KDS actualizada: incluye estado_item y numero_comanda, filtra solo ítems activos
CREATE OR REPLACE VIEW vw_kds_cocina AS
SELECT
    pd.id                                                   AS detalle_id,
    pd.empresa_id,
    p.id                                                    AS pedido_id,
    p.numero_orden,
    p.tipo_consumo,
    p.identificador_mesa_referencia                         AS mesa,
    p.estado_actual                                         AS estado_pedido,
    p.notas_generales,
    (p.created_at AT TIME ZONE 'UTC')::TIMESTAMPTZ          AS hora_ingreso,
    EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 60.0       AS minutos_transcurridos,
    pd.cantidad,
    pd.notas_preparacion,
    pr.id                                                   AS producto_id,
    pr.nombre                                               AS producto,
    pr.tiempo_preparacion_minutos,
    pd.estado_item,
    pd.numero_comanda
FROM pedidos_detalle pd
JOIN pedidos      p  ON pd.pedido_id   = p.id
JOIN productos    pr ON pd.producto_id = pr.id
WHERE p.estado_actual IN ('RECIBIDO', 'EN_PREPARACION')
  AND pd.estado_item  IN ('PENDIENTE', 'EN_PREPARACION');
