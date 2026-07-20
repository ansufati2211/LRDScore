-- =====================================================================
-- SCRIPT CORRECTIVO MANUAL — Ruta del Sabor  (ddl-auto=none, sin Flyway)
-- Ejecutar UNA sola vez sobre la BD (base del backup BKUP200726) con psql:
--   psql -U postgres -d lrds_db2 -f FIX_200726_kardex_y_reservas.sql
--
-- Corrige dos defectos que rompían el flujo real contra el esquema:
--   1. El CHECK de kardex_movimientos rechazaba los movimientos que el
--      backend Java emite (RESERVA, LIBERACION_RESERVA, CONSUMO_PRODUCCION),
--      por lo que "confirmar pedido" fallaba siempre con error 500.
--   2. sp_iniciar_preparacion descontaba stock_actual pero NO liberaba
--      stock_reservado -> fuga de reserva que degradaba las porciones.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Ampliar el catálogo de tipos de movimiento del kardex
-- ---------------------------------------------------------------------
ALTER TABLE public.kardex_movimientos
    DROP CONSTRAINT IF EXISTS kardex_movimientos_tipo_movimiento_check;

ALTER TABLE public.kardex_movimientos
    ADD CONSTRAINT kardex_movimientos_tipo_movimiento_check
    CHECK (((tipo_movimiento)::text = ANY ((ARRAY[
        'ENTRADA_COMPRA'::character varying,
        'ENTRADA_AJUSTE'::character varying,
        'SALIDA_PRODUCCION'::character varying,
        'SALIDA_MERMA'::character varying,
        'SALIDA_AJUSTE'::character varying,
        'INICIAL'::character varying,
        'RESERVA'::character varying,
        'LIBERACION_RESERVA'::character varying,
        'CONSUMO_PRODUCCION'::character varying
    ])::text[])));

-- ---------------------------------------------------------------------
-- 2. Reemplazar sp_iniciar_preparacion para que también libere la reserva
--    (stock_reservado) al convertirla en consumo real (stock_actual),
--    e ignore los ítems cancelados.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.sp_iniciar_preparacion(IN p_pedido_id bigint, IN p_usuario_id bigint)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_empresa_id BIGINT;
    v_sede_id BIGINT;
    v_estado_actual VARCHAR;
    v_req RECORD;
BEGIN
    SELECT empresa_id, sede_id, estado_actual
    INTO v_empresa_id, v_sede_id, v_estado_actual
    FROM pedidos WHERE id = p_pedido_id FOR UPDATE;

    IF v_estado_actual != 'RECIBIDO' THEN
        RAISE EXCEPTION 'El pedido debe estar en estado RECIBIDO para iniciar preparacion.';
    END IF;

    FOR v_req IN (
        SELECT rd.insumo_id, i.nombre, isede.stock_actual, isede.stock_reservado, isede.costo_unitario,
               SUM(rd.cantidad_requerida * pd.cantidad) AS total_necesario
        FROM pedidos_detalle pd
        JOIN productos p ON pd.producto_id = p.id
        JOIN receta_detalles rd ON p.id = rd.producto_id
        JOIN insumos i ON rd.insumo_id = i.id
        JOIN insumo_sede isede ON i.id = isede.insumo_id AND isede.sede_id = v_sede_id
        WHERE pd.pedido_id = p_pedido_id
          AND p.es_preparado = TRUE
          AND pd.estado_item <> 'CANCELADO'
        GROUP BY rd.insumo_id, i.nombre, isede.stock_actual, isede.stock_reservado, isede.costo_unitario
    ) LOOP
        IF v_req.stock_actual < v_req.total_necesario THEN
            RAISE EXCEPTION 'Stock insuficiente en esta sede para insumo: %. Requerido: %, Actual: %', v_req.nombre, v_req.total_necesario, v_req.stock_actual;
        END IF;

        -- Descuenta el stock fisico Y libera la reserva tomada en la confirmacion
        UPDATE insumo_sede
        SET stock_actual    = stock_actual - v_req.total_necesario,
            stock_reservado = GREATEST(stock_reservado - v_req.total_necesario, 0)
        WHERE insumo_id = v_req.insumo_id AND sede_id = v_sede_id;

        INSERT INTO kardex_movimientos (
            empresa_id, sede_id, insumo_id, tipo_movimiento, cantidad,
            stock_anterior, stock_posterior, costo_unitario, pedido_id, usuario_id, observacion
        )
        VALUES (
            v_empresa_id, v_sede_id, v_req.insumo_id, 'SALIDA_PRODUCCION', v_req.total_necesario,
            v_req.stock_actual, v_req.stock_actual - v_req.total_necesario, v_req.costo_unitario,
            p_pedido_id, p_usuario_id, 'Preparacion Pedido ' || p_pedido_id
        );
    END LOOP;

    UPDATE pedidos SET estado_actual = 'EN_PREPARACION' WHERE id = p_pedido_id;
END;
$$;

COMMIT;

ALTER TABLE kardex_movimientos ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
