-- =============================================================================
-- MÓDULO 0: Entitlement por Plan (Suscripción)
-- Aplicar sobre la BD lrds_db después de que V1 (esquema base) ya esté aplicado.
-- Orden: planes → plan_modulos → suscripciones → ALTER empresas
-- =============================================================================

-- 1. Tabla de planes de suscripción
CREATE TABLE IF NOT EXISTS planes (
    id               BIGSERIAL    PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    precio_mensual   NUMERIC(10,2) NOT NULL,
    estado_registro  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Módulos habilitados por plan (N:M desnormalizado como tabla propia)
--    codigo_modulo: PEDIDOS | CAJA | INVENTARIO | KDS |
--                  REPORTES_AVANZADOS | FACTURACION | RESERVAS | FIDELIZACION
CREATE TABLE IF NOT EXISTS plan_modulos (
    id             BIGSERIAL   PRIMARY KEY,
    plan_id        BIGINT      NOT NULL REFERENCES planes(id) ON DELETE CASCADE,
    codigo_modulo  VARCHAR(30) NOT NULL,
    CONSTRAINT uk_plan_modulo UNIQUE (plan_id, codigo_modulo)
);

-- 3. Suscripciones de empresa a plan
--    estado: ACTIVA | SUSPENDIDA | VENCIDA
CREATE TABLE IF NOT EXISTS suscripciones (
    id          BIGSERIAL    PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    plan_id     BIGINT       NOT NULL REFERENCES planes(id),
    estado      VARCHAR(20)  NOT NULL CHECK (estado IN ('ACTIVA','SUSPENDIDA','VENCIDA')),
    fecha_inicio DATE        NOT NULL,
    fecha_fin    DATE,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_suscripciones_empresa_estado
    ON suscripciones (empresa_id, estado, fecha_inicio DESC);

-- 4. FK en empresas → suscripción vigente (circular diferido para evitar deadlock de inserción)
ALTER TABLE empresas
    ADD COLUMN IF NOT EXISTS suscripcion_vigente_id BIGINT
        REFERENCES suscripciones(id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;

-- =============================================================================
-- DATOS SEMILLA: plan básico y plan completo
-- =============================================================================

INSERT INTO planes (nombre, precio_mensual) VALUES
    ('Básico',   49.00),
    ('Completo', 99.00)
ON CONFLICT DO NOTHING;

-- Plan Básico: solo módulos core
INSERT INTO plan_modulos (plan_id, codigo_modulo)
SELECT p.id, m.modulo
FROM   planes p,
       (VALUES ('PEDIDOS'),('CAJA'),('INVENTARIO'),('KDS')) AS m(modulo)
WHERE  p.nombre = 'Básico'
ON CONFLICT DO NOTHING;

-- Plan Completo: todos los módulos
INSERT INTO plan_modulos (plan_id, codigo_modulo)
SELECT p.id, m.modulo
FROM   planes p,
       (VALUES
           ('PEDIDOS'),('CAJA'),('INVENTARIO'),('KDS'),
           ('REPORTES_AVANZADOS'),('FACTURACION'),('RESERVAS'),('FIDELIZACION')
       ) AS m(modulo)
WHERE  p.nombre = 'Completo'
ON CONFLICT DO NOTHING;
