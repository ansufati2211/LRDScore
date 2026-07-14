-- =================================================================================
-- RESOLUCIÓN PUNTOS PENDIENTES 1: ACTUALIZACIÓN DE SP DE PREPARACIÓN
-- =================================================================================

CREATE OR REPLACE PROCEDURE sp_iniciar_preparacion(
    p_pedido_id BIGINT,
    p_usuario_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_empresa_id BIGINT;
    v_sede_id BIGINT;
    v_estado_actual VARCHAR;
    v_req RECORD;
BEGIN
    -- Obtenemos empresa_id, sede_id y el estado_actual del pedido
    SELECT empresa_id, sede_id, estado_actual 
    INTO v_empresa_id, v_sede_id, v_estado_actual 
    FROM pedidos WHERE id = p_pedido_id FOR UPDATE;

    IF v_estado_actual != 'RECIBIDO' THEN
        RAISE EXCEPTION 'El pedido debe estar en estado RECIBIDO para iniciar preparación.';
    END IF;

    -- Validar y descontar stock cruzando con insumo_sede
    FOR v_req IN (
        SELECT rd.insumo_id, i.nombre, isede.stock_actual, isede.costo_unitario, 
               SUM(rd.cantidad_requerida * pd.cantidad) as total_necesario
        FROM pedidos_detalle pd
        JOIN productos p ON pd.producto_id = p.id
        JOIN receta_detalles rd ON p.id = rd.producto_id
        JOIN insumos i ON rd.insumo_id = i.id
        JOIN insumo_sede isede ON i.id = isede.insumo_id AND isede.sede_id = v_sede_id
        WHERE pd.pedido_id = p_pedido_id AND p.es_preparado = TRUE
        GROUP BY rd.insumo_id, i.nombre, isede.stock_actual, isede.costo_unitario
    ) LOOP
        IF v_req.stock_actual < v_req.total_necesario THEN
            RAISE EXCEPTION 'Stock insuficiente en esta sede para insumo: %. Requerido: %, Actual: %', v_req.nombre, v_req.total_necesario, v_req.stock_actual;
        END IF;

        -- Actualizar Insumo exclusivamente en la sede correspondiente
        UPDATE insumo_sede 
        SET stock_actual = stock_actual - v_req.total_necesario 
        WHERE insumo_id = v_req.insumo_id AND sede_id = v_sede_id;

        -- Insertar en Kardex asegurando incluir el sede_id
        INSERT INTO kardex_movimientos (
            empresa_id, sede_id, insumo_id, tipo_movimiento, cantidad, 
            stock_anterior, stock_posterior, costo_unitario, pedido_id, usuario_id, observacion
        )
        VALUES (
            v_empresa_id, v_sede_id, v_req.insumo_id, 'SALIDA_PRODUCCION', v_req.total_necesario, 
            v_req.stock_actual, v_req.stock_actual - v_req.total_necesario, v_req.costo_unitario, 
            p_pedido_id, p_usuario_id, 'Preparación Pedido ' || p_pedido_id
        );
    END LOOP;

    -- Actualizar estado de pedido
    UPDATE pedidos SET estado_actual = 'EN_PREPARACION' WHERE id = p_pedido_id;
END;
$$;
-- =================================================================================
-- RESOLUCIÓN PUNTOS PENDIENTES 2: LIMPIEZA DE COLUMNAS DE INSUMOS
-- =================================================================================

ALTER TABLE insumos 
    DROP COLUMN IF EXISTS stock_actual,
    DROP COLUMN IF EXISTS stock_minimo,
    DROP COLUMN IF EXISTS stock_reservado,
    DROP COLUMN IF EXISTS costo_unitario;
    -- =================================================================================
-- RESOLUCIÓN PUNTOS PENDIENTES 3: NUEVA ESTRUCTURA DE ROLES
-- =================================================================================

-- 1. Migramos cualquier usuario que actualmente tenga el rol antiguo 'ROLE_GERENTE'
--    al nuevo rol 'ROLE_ADMIN_EMPRESA' para no perder su acceso a nivel cadena.
UPDATE usuarios 
SET rol = 'ROLE_ADMIN_EMPRESA' 
WHERE rol = 'ROLE_GERENTE';

-- 2. Eliminamos la restricción de validación anterior
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;

-- 3. Aplicamos la nueva restricción con los nuevos roles definidos
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check
CHECK (rol IN (
    'ROLE_SUPER_ADMIN',    
    'ROLE_ADMIN_EMPRESA',  
    'ROLE_GERENTE_SEDE',   
    'ROLE_CAJERO',         
    'ROLE_MOZO',           
    'ROLE_COCINA'          
));
-- =================================================================================
-- CORRECCIÓN FINAL: CORRELATIVOS POR SEDE Y VISTAS ANALÍTICAS
-- =================================================================================

-- 1. Actualizar el generador de órdenes para que sea por SEDE
CREATE OR REPLACE FUNCTION fn_generar_numero_orden()
RETURNS TRIGGER AS $$
BEGIN
    SELECT COALESCE(MAX(numero_orden), 0) + 1 INTO NEW.numero_orden
    FROM pedidos 
    WHERE empresa_id = NEW.empresa_id 
      AND sede_id = NEW.sede_id -- Filtro crítico añadido
      AND CAST(created_at AS DATE) = CURRENT_DATE;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Actualizar Vista: CONSUMO DE INSUMOS
DROP VIEW IF EXISTS vw_consumo_insumos;
CREATE VIEW vw_consumo_insumos AS
SELECT 
    ROW_NUMBER() OVER(ORDER BY CAST(km.created_at AS DATE), km.sede_id, km.insumo_id) AS id,
    km.empresa_id,
    km.sede_id, -- Añadido
    km.insumo_id,
    i.nombre AS insumo,
    i.unidad_medida,
    CAST(km.created_at AS DATE) AS fecha,
    SUM(km.cantidad) AS cantidad_consumida,
    SUM(km.cantidad * COALESCE(km.costo_unitario, 0)) AS costo_total_consumido
FROM kardex_movimientos km
JOIN insumos i ON km.insumo_id = i.id
WHERE km.tipo_movimiento = 'SALIDA_PRODUCCION'
GROUP BY CAST(km.created_at AS DATE), km.empresa_id, km.sede_id, km.insumo_id, i.nombre, i.unidad_medida;

-- 3. Actualizar Vista: HISTORIAL DE KARDEX
DROP VIEW IF EXISTS vw_kardex_insumo;
CREATE VIEW vw_kardex_insumo AS
SELECT 
    km.id AS movimiento_id,
    km.empresa_id,
    km.sede_id, -- Añadido
    km.insumo_id,
    i.nombre AS insumo,
    km.tipo_movimiento,
    km.cantidad,
    km.stock_anterior,
    km.stock_posterior,
    km.costo_unitario,
    km.pedido_id,
    km.usuario_id,
    u.nombre AS usuario_nombre,
    km.observacion,
    km.created_at AS fecha_movimiento
FROM kardex_movimientos km
JOIN insumos i ON km.insumo_id = i.id
LEFT JOIN usuarios u ON km.usuario_id = u.id;

-- 4. Actualizar Vista: PRODUCTOS MÁS VENDIDOS
DROP VIEW IF EXISTS vw_productos_mas_vendidos;
CREATE VIEW vw_productos_mas_vendidos AS
SELECT 
    ROW_NUMBER() OVER(ORDER BY SUM(pd.cantidad) DESC) AS id,
    pd.empresa_id,
    p.sede_id, -- Extraído del pedido asociado
    prod.nombre AS producto,
    SUM(pd.cantidad) AS total_vendido
FROM pedidos_detalle pd
JOIN pedidos p ON pd.pedido_id = p.id
JOIN productos prod ON pd.producto_id = prod.id
WHERE p.estado_actual IN ('PAGADO', 'ENTREGADO')
GROUP BY pd.empresa_id, p.sede_id, prod.nombre;

-- =================================================================================
-- PASO 1: INSERTS MAESTROS (Requeridos antes de hacer los UPDATES)
-- =================================================================================

-- 1. Crear la Sede Principal para la Empresa 1
INSERT INTO sedes (id, empresa_id, codigo_establecimiento, nombre, direccion)
VALUES (1, 1, '0001', 'Sede Cañete Principal', 'Plaza de Armas')
ON CONFLICT (empresa_id, id) DO NOTHING;

-- 2. Vincular los usuarios existentes a la Sede 1 para que puedan operar
-- Asume que tus usuarios (Admin, Mozo, Cajero) tienen los IDs 1, 2 y 3.
INSERT INTO usuario_sedes (usuario_id, sede_id, empresa_id) VALUES 
(1, 1, 1), 
(2, 1, 1), 
(3, 1, 1)
ON CONFLICT (usuario_id, sede_id) DO NOTHING;

-- =================================================================================
-- PASO 2: UPDATES A LA DATA YA INYECTADA (Cerrando los registros huérfanos)
-- =================================================================================

-- 3. Actualizar roles antiguos si existieran en la data inyectada
UPDATE usuarios 
SET rol = 'ROLE_ADMIN_EMPRESA' 
WHERE rol = 'ROLE_GERENTE';

-- 4. Asignar la Sede 1 a todas las operaciones históricas del Kardex
UPDATE kardex_movimientos 
SET sede_id = 1 
WHERE sede_id IS NULL AND empresa_id = 1;

-- 5. Asignar la Sede 1 a todos los pedidos y cajas anteriores
UPDATE pedidos SET sede_id = 1 WHERE sede_id IS NULL AND empresa_id = 1;
UPDATE sesiones_caja SET sede_id = 1 WHERE sede_id IS NULL AND empresa_id = 1;
UPDATE documentos_cobro SET sede_id = 1 WHERE sede_id IS NULL AND empresa_id = 1;
UPDATE documentos_venta SET sede_id = 1 WHERE sede_id IS NULL AND empresa_id = 1;

-- 6. Sincronizar estado_item en pedidos_detalle (por si había pedidos en curso)
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

-- =================================================================================
-- PASO 3: INSERTS MODIFICADOS (Trasladando inventario y facturación)
-- =================================================================================

-- 7. Crear el stock físico en la sede (migrando el concepto de stock del catálogo a la sucursal)
INSERT INTO insumo_sede (insumo_id, sede_id, empresa_id, stock_actual, stock_minimo, costo_unitario) VALUES
(1, 1, 1, 5000.00, 1000.00, 0.05),
(2, 1, 1, 10000.00, 2000.00, 0.01),
(3, 1, 1, 100.00, 20.00, 0.50),
(4, 1, 1, 2000.00, 500.00, 0.02)
ON CONFLICT (insumo_id, sede_id) DO UPDATE 
SET stock_actual = EXCLUDED.stock_actual;

-- 8. Inicializar las series correlativas de la sede para probar la Facturación
INSERT INTO series_correlativo (empresa_id, sede_id, tipo, serie, ultimo_correlativo) VALUES 
(1, 1, 'BOLETA', 'B001', 0),
(1, 1, 'FACTURA', 'F001', 0),
(1, 1, 'NOTA_VENTA', 'NV01', 0)
ON CONFLICT (sede_id, tipo) DO NOTHING;

-- =================================================================================
-- INSERTS ESTRATÉGICOS PARA ESCENARIO MULTI-SEDE (DATA DE PRUEBA)
-- =================================================================================

-- 1. CREAR UNA SEGUNDA SEDE (Para probar el aislamiento de datos)
INSERT INTO sedes (id, empresa_id, codigo_establecimiento, nombre, direccion)
VALUES (2, 1, '0002', 'Sede Parcona', 'Av. Principal, Parcona, Ica')
ON CONFLICT (empresa_id, id) DO NOTHING;

-- 2. CREAR NUEVOS USUARIOS PARA LA NUEVA SEDE
-- ID 4: Gerente de Sede (Solo ve Parcona)
-- ID 5 y 6: Operativos de Parcona (Mozo y Cocina)
INSERT INTO usuarios (id, empresa_id, nombre, correo, password_hash, rol) VALUES 
(4, 1, 'Jesus Rojas', 'jesus.rojas@rutadelsabor.com', '$2a$10$X21e50L4ZY/rqvQsQDwLIujv03vnRyNSz0rZGvJYpMa2i/Ps3.FlC', 'ROLE_GERENTE_SEDE'),
(5, 1, 'Mozo Fran', 'fran@rutadelsabor.com', '$2a$10$X21e50L4ZY/rqvQsQDwLIujv03vnRyNSz0rZGvJYpMa2i/Ps3.FlC', 'ROLE_MOZO'),
(6, 1, 'Cocina Thiago', 'thiago@rutadelsabor.com', '$2a$10$X21e50L4ZY/rqvQsQDwLIujv03vnRyNSz0rZGvJYpMa2i/Ps3.FlC', 'ROLE_COCINA')
ON CONFLICT DO NOTHING;

-- 3. ASIGNAR USUARIOS A LA SEDE PARCONA (Tabla Intermedia)
-- Nota: Gabriel (ID 1) como ROLE_SUPER_ADMIN no necesita estar aquí para ver todo, 
-- pero vincularemos a Jesus, Fran y Thiago estrictamente a la Sede 2.
INSERT INTO usuario_sedes (usuario_id, sede_id, empresa_id) VALUES 
(4, 2, 1), 
(5, 2, 1), 
(6, 2, 1)
ON CONFLICT (usuario_id, sede_id) DO NOTHING;

-- 4. INVENTARIO INDEPENDIENTE PARA LA SEDE PARCONA
-- Se insertan los mismos insumos (1,2,3,4) pero con cantidades totalmente distintas a las de Cañete
INSERT INTO insumo_sede (insumo_id, sede_id, empresa_id, stock_actual, stock_minimo, costo_unitario) VALUES
(1, 2, 1, 1500.00, 500.00, 0.06),  -- Menos café en Parcona, costo ligeramente distinto
(2, 2, 1, 3000.00, 1000.00, 0.02), -- Menos leche
(3, 2, 1, 50.00, 15.00, 0.55),     -- Pan
(4, 2, 1, 800.00, 200.00, 0.03)    -- Jamón
ON CONFLICT (insumo_id, sede_id) DO NOTHING;

-- 5. MOVIMIENTOS INICIALES EN EL KARDEX PARA LA SEDE PARCONA
INSERT INTO kardex_movimientos (empresa_id, sede_id, insumo_id, tipo_movimiento, cantidad, stock_anterior, stock_posterior, costo_unitario, usuario_id, observacion) VALUES
(1, 2, 1, 'INICIAL', 1500.00, 0.00, 1500.00, 0.06, 4, 'Carga inicial Sede Parcona'),
(1, 2, 2, 'INICIAL', 3000.00, 0.00, 3000.00, 0.02, 4, 'Carga inicial Sede Parcona'),
(1, 2, 3, 'INICIAL', 50.00, 0.00, 50.00, 0.55, 4, 'Carga inicial Sede Parcona'),
(1, 2, 4, 'INICIAL', 800.00, 0.00, 800.00, 0.03, 4, 'Carga inicial Sede Parcona')
ON CONFLICT DO NOTHING;

-- 6. SERIES DE FACTURACIÓN INDEPENDIENTES (Crítico para el Módulo 7)
-- La sede 2 emite sus propios comprobantes con serie distinta (ej. B002, F002)
INSERT INTO series_correlativo (empresa_id, sede_id, tipo, serie, ultimo_correlativo) VALUES 
(1, 2, 'BOLETA', 'B002', 0),
(1, 2, 'FACTURA', 'F002', 0),
(1, 2, 'NOTA_VENTA', 'NV02', 0)
ON CONFLICT (sede_id, tipo) DO NOTHING;

-- 7. CLIENTES DE PRUEBA (Compartidos a nivel empresa, pueden comprar en cualquier sede)
INSERT INTO clientes (id, empresa_id, nombre_razon_social, tipo_documento, numero_documento, correo) VALUES
(1, 1, 'Santy', 'DNI', '70123456', 'santy@correo.com'),
(2, 1, 'Pablo', 'DNI', '70654321', 'pablo@correo.com'),
(3, 1, 'Ignacio', 'DNI', '70987654', 'ignacio@correo.com')
ON CONFLICT DO NOTHING;

DELETE FROM usuario_sedes WHERE usuario_id = 1;

=========================================================
? CONTRASE�A ORIGINAL : admin123
?? HASH BCRYPT GENERADO: $2a$10$dI19A52n36xMgshgo0pWTOlBsXgPo7S3LFdRlM6NgK3Bo.qPrqim.
=========================================================

