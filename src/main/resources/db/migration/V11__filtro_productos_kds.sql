
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
    (p.created_at AT TIME ZONE 'America/Lima')::TIMESTAMPTZ AS hora_ingreso,
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
  AND pd.estado_item IN ('PENDIENTE', 'EN_PREPARACION')
  AND pr.es_preparado = TRUE; 