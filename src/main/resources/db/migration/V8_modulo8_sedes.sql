-- =================================================================================
-- MÓDULO 8: SEDES (Multi-sede por empresa)
-- Aplicar DESPUÉS de que 00_SCRIPT-COMPLETO.sql (con Módulo 0, 3, 4, 5, 6, 7) ya
-- esté aplicado sobre la BD lrds_db.
--
-- Principio de diseño: toda tabla que quede ligada a una sede usa FK COMPUESTA
-- (empresa_id, sede_id) -> sedes(empresa_id, id). Esto hace IMPOSIBLE a nivel de
-- base de datos que un registro de la Empresa A quede apuntando a una sede de la
-- Empresa B — la garantía vive en el schema, no depende de que el código Java
-- valide correctamente.
--
-- Orden de este script:
--   PARTE 1: Tabla sedes + límite de sedes por plan
--   PARTE 2: División de insumos -> insumo_sede (catálogo vs. stock físico)
--   PARTE 3: usuario_sedes (N:M, con reglas de rol)
--   PARTE 4: ALTER de sede_id en tablas operativas existentes
--   PARTE 5: series_correlativo -> cambio de PK a (sede_id, tipo)
--   PARTE 6: MIGRACIÓN DE DATOS (sede principal por empresa + backfill)
--   PARTE 7: NOT NULL finales + índices
--   PARTE 8: Ajuste de vistas para reporte consolidado vs. por sede
-- =================================================================================


-- =================================================================================
-- PARTE 1: TABLA SEDES + LÍMITE DE SEDES POR PLAN
-- =================================================================================

CREATE TABLE IF NOT EXISTS sedes (
    id                      BIGSERIAL     PRIMARY KEY,
    empresa_id              BIGINT        NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    codigo_establecimiento  VARCHAR(10),   -- código SUNAT del establecimiento (ej. "0001")
    nombre                  VARCHAR(100)  NOT NULL,
    direccion               VARCHAR(255),
    estado_registro         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Requisito para que otras tablas puedan usar FK compuesta (empresa_id, sede_id)
    CONSTRAINT uq_sedes_empresa_id UNIQUE (empresa_id, id)
);

CREATE TRIGGER trg_sedes_upd BEFORE UPDATE ON sedes
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE INDEX IF NOT EXISTS idx_sedes_empresa ON sedes(empresa_id, estado_registro);

-- Límite de sedes según plan contratado (NULL = ilimitado)
ALTER TABLE planes ADD COLUMN IF NOT EXISTS max_sedes INTEGER;

-- Sugerencia de valores (ajustar si el negocio decide otra cosa):
UPDATE planes SET max_sedes = 1  WHERE nombre = 'Básico'   AND max_sedes IS NULL;
UPDATE planes SET max_sedes = NULL WHERE nombre = 'Completo'; -- ilimitado


-- =================================================================================
-- PARTE 2: DIVISIÓN DE INSUMOS -> insumo_sede
-- insumos queda como catálogo maestro (a nivel empresa). El stock físico real
-- (que sí varía por local) se mueve a insumo_sede.
-- =================================================================================

-- Requisito para FK compuesta (empresa_id, insumo_id) desde insumo_sede
ALTER TABLE insumos ADD CONSTRAINT uq_insumos_empresa_id UNIQUE (empresa_id, id);

CREATE TABLE IF NOT EXISTS insumo_sede (
    insumo_id        BIGINT         NOT NULL REFERENCES insumos(id) ON DELETE CASCADE,
    sede_id          BIGINT         NOT NULL,
    empresa_id       BIGINT         NOT NULL,
    stock_actual     NUMERIC(12,3)  NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo     NUMERIC(12,3)  DEFAULT 0,
    stock_reservado  NUMERIC(12,3)  NOT NULL DEFAULT 0,
    costo_unitario   NUMERIC(10,2)  DEFAULT 0.00,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    PRIMARY KEY (insumo_id, sede_id),

    -- El insumo debe pertenecer a la misma empresa que la sede (consistencia de tenant)
    CONSTRAINT fk_insumo_sede_insumo  FOREIGN KEY (empresa_id, insumo_id) REFERENCES insumos(empresa_id, id),
    CONSTRAINT fk_insumo_sede_sede    FOREIGN KEY (empresa_id, sede_id)   REFERENCES sedes(empresa_id, id)
);

CREATE TRIGGER trg_insumo_sede_upd BEFORE UPDATE ON insumo_sede
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE INDEX IF NOT EXISTS idx_insumo_sede_sede ON insumo_sede(sede_id);


-- =================================================================================
-- PARTE 3: usuario_sedes (N:M) — un usuario puede atender varias sedes
-- Regla de negocio (se valida en el service layer, no en SQL):
--   ROLE_SUPER_ADMIN / ROLE_GERENTE -> sin fila obligatoria, ven todas las sedes
--   de su empresa (acceso consolidado).
--   ROLE_CAJERO / ROLE_MOZO / ROLE_COCINA -> deben tener al menos una fila activa.
-- =================================================================================

ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_empresa_id UNIQUE (empresa_id, id);

CREATE TABLE IF NOT EXISTS usuario_sedes (
    usuario_id  BIGINT  NOT NULL,
    sede_id     BIGINT  NOT NULL,
    empresa_id  BIGINT  NOT NULL,
    estado_registro BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    PRIMARY KEY (usuario_id, sede_id),

    CONSTRAINT fk_usuario_sedes_usuario FOREIGN KEY (empresa_id, usuario_id) REFERENCES usuarios(empresa_id, id),
    CONSTRAINT fk_usuario_sedes_sede    FOREIGN KEY (empresa_id, sede_id)    REFERENCES sedes(empresa_id, id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_sedes_sede ON usuario_sedes(sede_id);


-- =================================================================================
-- PARTE 4: ALTER DE sede_id EN TABLAS OPERATIVAS EXISTENTES
-- Se agregan NULLABLE primero; se rellenan en la PARTE 6 y se cierran a NOT NULL
-- en la PARTE 7 (patrón seguro para no romper filas ya existentes).
-- =================================================================================

ALTER TABLE pedidos            ADD COLUMN IF NOT EXISTS sede_id BIGINT;
ALTER TABLE sesiones_caja      ADD COLUMN IF NOT EXISTS sede_id BIGINT;
ALTER TABLE kardex_movimientos ADD COLUMN IF NOT EXISTS sede_id BIGINT;
ALTER TABLE documentos_cobro   ADD COLUMN IF NOT EXISTS sede_id BIGINT;
ALTER TABLE documentos_venta   ADD COLUMN IF NOT EXISTS sede_id BIGINT;

-- FKs compuestas hacia sedes (garantizan que la sede pertenezca a la misma empresa)
ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedidos_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

ALTER TABLE sesiones_caja
    ADD CONSTRAINT fk_sesiones_caja_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

ALTER TABLE kardex_movimientos
    ADD CONSTRAINT fk_kardex_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

ALTER TABLE documentos_cobro
    ADD CONSTRAINT fk_documentos_cobro_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

ALTER TABLE documentos_venta
    ADD CONSTRAINT fk_documentos_venta_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

-- Consistencia extra: un movimiento de kardex debe referenciar un insumo QUE YA
-- TIENE fila de stock en esa sede (insumo_sede). Evita mermas/consumos "fantasma".
ALTER TABLE kardex_movimientos
    ADD CONSTRAINT fk_kardex_insumo_sede FOREIGN KEY (insumo_id, sede_id) REFERENCES insumo_sede(insumo_id, sede_id);


-- =================================================================================
-- PARTE 5: series_correlativo -> CAMBIO DE PK A (sede_id, tipo)
-- SUNAT numera boletas/facturas por establecimiento físico, no por empresa.
-- =================================================================================

ALTER TABLE series_correlativo ADD COLUMN IF NOT EXISTS sede_id BIGINT;

-- (El backfill de sede_id ocurre en la PARTE 6; aquí solo dejamos la estructura lista)

ALTER TABLE series_correlativo DROP CONSTRAINT IF EXISTS series_correlativo_pkey;

-- La nueva PK y la FK compuesta se agregan en la PARTE 7, después del backfill,
-- porque una PK no admite NULLs y aún no hemos poblado sede_id.


-- =================================================================================
-- PARTE 6: MIGRACIÓN DE DATOS EXISTENTES
-- Crea una "Sede Principal" por cada empresa existente y rellena sede_id en todas
-- las tablas operativas, además de mover el stock de insumos a insumo_sede.
-- =================================================================================

DO $$
DECLARE
    v_empresa RECORD;
    v_sede_id BIGINT;
BEGIN
    FOR v_empresa IN SELECT id FROM empresas LOOP

        -- Crear sede principal si la empresa aún no tiene ninguna
        IF NOT EXISTS (SELECT 1 FROM sedes WHERE empresa_id = v_empresa.id) THEN
            INSERT INTO sedes (empresa_id, nombre, codigo_establecimiento)
            VALUES (v_empresa.id, 'Sede Principal', '0001')
            RETURNING id INTO v_sede_id;
        ELSE
            SELECT id INTO v_sede_id FROM sedes WHERE empresa_id = v_empresa.id ORDER BY id LIMIT 1;
        END IF;

        -- Migrar stock de insumos (catálogo) a insumo_sede (stock físico por sede)
        -- IMPORTANTE: esto debe ocurrir ANTES de backfillear kardex_movimientos.sede_id,
        -- porque la FK compuesta fk_kardex_insumo_sede exige que la fila (insumo_id,
        -- sede_id) ya exista en insumo_sede.
        INSERT INTO insumo_sede (insumo_id, sede_id, empresa_id, stock_actual, stock_minimo, stock_reservado, costo_unitario)
        SELECT i.id, v_sede_id, v_empresa.id, i.stock_actual, i.stock_minimo, i.stock_reservado, i.costo_unitario
        FROM insumos i
        WHERE i.empresa_id = v_empresa.id
        ON CONFLICT (insumo_id, sede_id) DO NOTHING;

        -- Backfill de sede_id en tablas operativas
        UPDATE pedidos            SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;
        UPDATE sesiones_caja      SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;
        UPDATE kardex_movimientos SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;
        UPDATE documentos_cobro   SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;
        UPDATE documentos_venta   SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;
        UPDATE series_correlativo SET sede_id = v_sede_id WHERE empresa_id = v_empresa.id AND sede_id IS NULL;

        -- Asignar todos los usuarios operativos existentes de la empresa a la sede principal
        INSERT INTO usuario_sedes (usuario_id, sede_id, empresa_id)
        SELECT u.id, v_sede_id, v_empresa.id
        FROM usuarios u
        WHERE u.empresa_id = v_empresa.id
          AND u.rol IN ('ROLE_CAJERO', 'ROLE_MOZO', 'ROLE_COCINA')
        ON CONFLICT (usuario_id, sede_id) DO NOTHING;

    END LOOP;
END $$;


-- =================================================================================
-- PARTE 7: NOT NULL FINALES, PK DE series_correlativo E ÍNDICES
-- =================================================================================

ALTER TABLE pedidos            ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE sesiones_caja      ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE kardex_movimientos ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE documentos_cobro   ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE documentos_venta   ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE series_correlativo ALTER COLUMN sede_id SET NOT NULL;

-- Nueva PK de series_correlativo: correlativo por sede y tipo de documento
ALTER TABLE series_correlativo ADD CONSTRAINT series_correlativo_pkey PRIMARY KEY (sede_id, tipo);
ALTER TABLE series_correlativo
    ADD CONSTRAINT fk_series_correlativo_sede FOREIGN KEY (empresa_id, sede_id) REFERENCES sedes(empresa_id, id);

-- Alinear el unique de documentos_venta con el nuevo esquema por sede
ALTER TABLE documentos_venta DROP CONSTRAINT IF EXISTS uq_dv_serie_correlativo;
ALTER TABLE documentos_venta
    ADD CONSTRAINT uq_dv_serie_correlativo UNIQUE (sede_id, tipo, serie, correlativo);

CREATE INDEX IF NOT EXISTS idx_pedidos_sede            ON pedidos(sede_id, estado_actual);
CREATE INDEX IF NOT EXISTS idx_sesiones_caja_sede       ON sesiones_caja(sede_id, estado);
CREATE INDEX IF NOT EXISTS idx_kardex_sede              ON kardex_movimientos(sede_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documentos_venta_sede    ON documentos_venta(sede_id, tipo);


-- =================================================================================
-- PARTE 8: AJUSTE DE VISTAS — REPORTE CONSOLIDADO VS. POR SEDE
-- Mismo dato, misma tabla, dos formas de agrupar: nunca hay riesgo de que el
-- reporte consolidado y el reporte por sede queden desincronizados.
-- =================================================================================

-- Dashboard de ventas: ahora incluye sede_id. Para el consolidado de toda la
-- empresa, el consumidor de la vista hace GROUP BY empresa_id (ignorando sede_id);
-- para el reporte por sede, agrupa también por sede_id.
-- NOTA: se usa DROP + CREATE (no CREATE OR REPLACE) porque Postgres no permite
-- reordenar columnas de una vista existente, solo agregar al final.
DROP VIEW IF EXISTS vw_dashboard_ventas;
CREATE VIEW vw_dashboard_ventas AS
SELECT
    ROW_NUMBER() OVER (ORDER BY CAST(created_at AS DATE), empresa_id, sede_id) AS id,
    CAST(created_at AS DATE) AS fecha,
    empresa_id,
    sede_id,
    SUM(total)   AS total_ingresos,
    COUNT(id)    AS cantidad_pedidos
FROM pedidos
WHERE estado_actual IN ('PAGADO', 'ENTREGADO')
GROUP BY CAST(created_at AS DATE), empresa_id, sede_id;

-- KDS: agrega sede_id para que cada local vea solo lo suyo en el tablero de cocina
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
    (p.created_at AT TIME ZONE 'UTC')::TIMESTAMPTZ          AS hora_ingreso,
    EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 60.0       AS minutos_transcurridos,
    pd.cantidad,
    pd.notas_preparacion,
    pr.id                                                   AS producto_id,
    pr.nombre                                                AS producto,
    pr.tiempo_preparacion_minutos,
    pd.estado_item,
    pd.numero_comanda
FROM pedidos_detalle pd
JOIN pedidos      p  ON pd.pedido_id   = p.id
JOIN productos    pr ON pd.producto_id = pr.id
WHERE p.estado_actual IN ('RECIBIDO', 'EN_PREPARACION')
  AND pd.estado_item  IN ('PENDIENTE', 'EN_PREPARACION');

-- Stock crítico: ahora por sede, ya que el stock físico vive en insumo_sede
DROP VIEW IF EXISTS vw_stock_critico;
CREATE VIEW vw_stock_critico AS
SELECT
    ins.id, ins.empresa_id, isede.sede_id, ins.nombre,
    isede.stock_actual, isede.stock_minimo, ins.unidad_medida
FROM insumos ins
JOIN insumo_sede isede ON isede.insumo_id = ins.id
WHERE isede.stock_actual <= isede.stock_minimo AND ins.estado_registro = TRUE;

