-- MÓDULO 7: DocumentoVenta (Nota de Venta / Boleta / Factura)

-- Contador atómico de correlativo por (empresa, tipo) — sin huecos (R7-1)
-- No extiende BaseTenantEntity: se gestiona vía EntityManager con ON CONFLICT DO UPDATE
CREATE TABLE IF NOT EXISTS series_correlativo (
    empresa_id           BIGINT       NOT NULL,
    tipo                 VARCHAR(15)  NOT NULL,
    serie                VARCHAR(10)  NOT NULL,
    ultimo_correlativo   INTEGER      NOT NULL DEFAULT 0,
    updated_at           TIMESTAMP             DEFAULT NOW(),
    PRIMARY KEY (empresa_id, tipo)
);

-- Comprobantes de venta — soporta nota interna, boleta y factura sin rediseñar (R7-3)
CREATE TABLE IF NOT EXISTS documentos_venta (
    id                          BIGSERIAL     PRIMARY KEY,
    empresa_id                  BIGINT        NOT NULL,
    pedido_id                   BIGINT        REFERENCES pedidos(id),
    documento_cobro_id          BIGINT        REFERENCES documentos_cobro(id),

    -- Identificación tributaria
    tipo                        VARCHAR(15)   NOT NULL,   -- NOTA_VENTA | BOLETA | FACTURA
    serie                       VARCHAR(10)   NOT NULL,
    correlativo                 INTEGER       NOT NULL,

    -- Receptor — opcional en nota, obligatorio en boleta/factura; RUC en factura (R7-4)
    tipo_documento_receptor     VARCHAR(20),
    numero_documento_receptor   VARCHAR(20),
    razon_social_receptor       VARCHAR(200),

    -- Importes (los de boleta/factura incluyen IGV peruano 18 %)
    subtotal                    NUMERIC(12,2) NOT NULL DEFAULT 0,
    igv                         NUMERIC(12,2) NOT NULL DEFAULT 0,
    total                       NUMERIC(12,2) NOT NULL DEFAULT 0,

    -- Ciclo de vida — estados extra preparados para integración SUNAT futura (R7-3)
    estado_emision              VARCHAR(20)   NOT NULL DEFAULT 'EMITIDO',
    fecha_emision               TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- E7-3: anulación como estado + referencia — nunca se borra el comprobante
    motivo_anulacion            TEXT,
    documento_referencia_id     BIGINT        REFERENCES documentos_venta(id),

    created_at                  TIMESTAMP              DEFAULT NOW(),
    updated_at                  TIMESTAMP              DEFAULT NOW(),

    CONSTRAINT uq_dv_serie_correlativo UNIQUE (empresa_id, tipo, serie, correlativo)
);

CREATE INDEX IF NOT EXISTS idx_dv_pedido        ON documentos_venta (pedido_id)          WHERE pedido_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dv_doc_cobro     ON documentos_venta (documento_cobro_id) WHERE documento_cobro_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dv_empresa_tipo  ON documentos_venta (empresa_id, tipo);
