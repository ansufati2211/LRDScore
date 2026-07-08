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

    // Se ejecuta de forma asíncrona cada minuto (60000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void alertarPedidosDemorados() {
        // Tolerancia: 20 minutos desde la creación
        LocalDateTime limiteTolerancia = LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(20);

        // Búsqueda global a nivel de base de datos (ignora el TenantContext)
        List<Pedido> demorados = pedidoRepository.findByEstadoActualAndCreatedAtBefore(
                EstadoPedido.EN_PREPARACION, limiteTolerancia);

        for (Pedido pedido : demorados) {
            // FIX CRÍTICO: Extracción directa desde la entidad para enrutar el evento
            Long empresaId = pedido.getEmpresaId();
            Long sedeId = pedido.getSedeId();
                Object numeroOrden = pedido.getNumeroOrden() != null
                    ? pedido.getNumeroOrden()
                    : String.valueOf(pedido.getId());

            Map<String, Object> payload = Map.of(
                    "pedidoId", pedido.getId(),
                    "sedeId", sedeId, // Vital para que el Frontend de React filtre la alerta
                    "numeroOrden", numeroOrden,
                    "mensaje", "¡Alerta! Pedido superó el tiempo máximo de preparación."
            );

            // Emitir evento Server-Sent Events (SSE) a los roles correspondientes
            sseEmitterManager.publicarPorRol(empresaId, "ROLE_COCINA", "ALERTA_DEMORA", payload);
            sseEmitterManager.publicarPorRol(empresaId, "ROLE_GERENTE_SEDE", "ALERTA_DEMORA", payload);
        }
    }
}