package com.rutadelsabor.core.services;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class EscalacionScheduler {

    private final PedidoRepository pedidoRepository;
    private final SseEmitterManager sseEmitterManager;

    public EscalacionScheduler(PedidoRepository pedidoRepository, SseEmitterManager sseEmitterManager) {
        this.pedidoRepository = pedidoRepository;
        this.sseEmitterManager = sseEmitterManager;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void alertarPedidosDemorados() {
        LocalDateTime limiteTolerancia = LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(20);

        List<Pedido> demorados = pedidoRepository.findByEstadoActualAndCreatedAtBefore(
                EstadoPedido.EN_PREPARACION, limiteTolerancia);

        for (Pedido pedido : demorados) {
            Long empresaId = pedido.getEmpresaId();
            Long sedeId = pedido.getSedeId();
            Object numeroOrden = pedido.getNumeroOrden() != null
                    ? pedido.getNumeroOrden()
                    : String.valueOf(pedido.getId());

            Map<String, Object> payload = Map.of(
                    "pedidoId", pedido.getId(),
                    "sedeId", sedeId, 
                    "numeroOrden", numeroOrden,
                    "mensaje", "¡Alerta! Pedido superó el tiempo máximo de preparación."
            );

            // FASE 7: Emite la alarma EXCLUSIVAMENTE a la cocina y gerente de ese local específico
            sseEmitterManager.publicarPorRolYSede(empresaId, "ROLE_COCINA", sedeId, "ALERTA_DEMORA", payload);
            sseEmitterManager.publicarPorRolYSede(empresaId, "ROLE_GERENTE_SEDE", sedeId, "ALERTA_DEMORA", payload);
        }
    }
}