package com.rutadelsabor.core.services;

import com.rutadelsabor.core.config.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class EscalacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalacionScheduler.class);

    // R2-4: niveles de escalación ya enviados por pedido (1=sala, 2=caja, 3=gerencia).
    // Limpiado automáticamente cuando el pedido sale del estado LISTO.
    private final ConcurrentHashMap<Long, Set<Integer>> escalacionesEnviadas = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbc;
    private final SseEmitterManager sseEmitterManager;

    public EscalacionScheduler(JdbcTemplate jdbc, SseEmitterManager sseEmitterManager) {
        this.jdbc = jdbc;
        this.sseEmitterManager = sseEmitterManager;
    }

    // R2-7: corre server-side cada 30 s independientemente de clientes conectados.
    // SQL nativo: bypasses el filtro @TenantId de Hibernate para barrer todos los tenants.
    @Scheduled(fixedDelay = 30_000)
    public void escalarPedidosListos() {
        List<PedidoListoRow> activos = jdbc.query(
                "SELECT id, empresa_id, mozo_id, numero_orden, " +
                "identificador_mesa_referencia, tipo_consumo, updated_at " +
                "FROM pedidos WHERE estado_actual = 'LISTO'",
                (rs, n) -> new PedidoListoRow(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        rs.getLong("mozo_id"),
                        rs.getObject("numero_orden", Integer.class),
                        rs.getString("identificador_mesa_referencia"),
                        rs.getString("tipo_consumo"),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                )
        );

        // R2-4: elimina entradas de pedidos que ya salieron del estado LISTO
        Set<Long> activosIds = activos.stream().map(PedidoListoRow::id).collect(Collectors.toSet());
        escalacionesEnviadas.keySet().retainAll(activosIds);

        for (PedidoListoRow row : activos) {
            long min = ChronoUnit.MINUTES.between(row.updatedAt(), LocalDateTime.now());
            Set<Integer> enviados = escalacionesEnviadas.computeIfAbsent(row.id(), k -> ConcurrentHashMap.newKeySet());

            Map<String, Object> payload = Map.of(
                    "pedidoId", row.id(),
                    "numeroOrden", row.numeroOrden() != null ? row.numeroOrden() : row.id(),
                    "mesa", row.mesa() != null ? row.mesa() : "",
                    "tipoConsumo", row.tipoConsumo() != null ? row.tipoConsumo() : ""
            );

            // t >= 5 min → GERENTE/SUPER_ADMIN (plano vertical, R2)
            if (min >= 5 && enviados.add(3)) {
                log.info("Escalación nivel 3 (GERENCIA): pedido {} — {} min en LISTO, empresa {}", row.id(), min, row.empresaId());
                sseEmitterManager.publicarPorRol(row.empresaId(), "ROLE_GERENTE", "AVISO_PEDIDO_LISTO", payload);
                sseEmitterManager.publicarPorRol(row.empresaId(), "ROLE_SUPER_ADMIN", "AVISO_PEDIDO_LISTO", payload);
            }

            // t >= 2 min → CAJERO (plano horizontal nivel 2, R2)
            if (min >= 2 && enviados.add(2)) {
                log.info("Escalación nivel 2 (CAJA): pedido {} — {} min en LISTO, empresa {}", row.id(), min, row.empresaId());
                sseEmitterManager.publicarPorRol(row.empresaId(), "ROLE_CAJERO", "AVISO_PEDIDO_LISTO", payload);
            }

            // t >= 1 min → todos los MOZOs del tenant (plano horizontal nivel 1, R2)
            if (min >= 1 && enviados.add(1)) {
                log.info("Escalación nivel 1 (SALA): pedido {} — {} min en LISTO, empresa {}", row.id(), min, row.empresaId());
                sseEmitterManager.publicarPorRol(row.empresaId(), "ROLE_MOZO", "AVISO_PEDIDO_LISTO", payload);
            }
        }
    }

    record PedidoListoRow(Long id, Long empresaId, Long mozoId, Integer numeroOrden,
                          String mesa, String tipoConsumo, LocalDateTime updatedAt) {}
}
