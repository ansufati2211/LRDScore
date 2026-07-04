package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.PedidoDetalle;
import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.repositories.VwKdsCocinaRepository;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KdsServiceImpl implements IKdsService {

    private static final String KEY_PEDIDO_ID = "pedidoId";
    private static final String KEY_NUMERO_ORDEN = "numeroOrden";

    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;
    private final SseEmitterManager sseEmitterManager;
    private final IInventarioService inventarioService;

    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository,
                          PedidoRepository pedidoRepository,
                          SseEmitterManager sseEmitterManager,
                          IInventarioService inventarioService) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        return kdsRepository.findAll();
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con ID " + pedidoId + " no encontrado"));

        if (pedido.getEstadoActual() == EstadoPedido.PAGADO || pedido.getEstadoActual() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se puede iniciar preparación en un pedido " + pedido.getEstadoActual() + ".");
        }

        // Tomar solo los ítems PENDIENTE de este pedido (puede ser 2ª comanda añadida a un pedido ya en curso)
        List<PedidoDetalle> itemsPendientes = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.PENDIENTE)
                .collect(Collectors.toList());

        if (itemsPendientes.isEmpty()) {
            throw new ReglaNegocioException("No hay ítems pendientes para iniciar preparación.");
        }

        Long empresaId = TenantContext.getCurrentTenant();

        // Recipe-based: evita doble procesamiento de RESERVA cuando hay múltiples comandas (Módulo 4)
        boolean requiereRevision = inventarioService.convertirItemsAConsumo(pedidoId, itemsPendientes);

        itemsPendientes.forEach(d -> d.setEstadoItem(EstadoItem.EN_PREPARACION));

        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));

        if (requiereRevision) {
            pedido.setRequiereRevision(true);
            sseEmitterManager.publicarPorRol(empresaId, "ROLE_GERENTE", "PEDIDO_REQUIERE_REVISION", Map.of(
                    KEY_PEDIDO_ID, pedidoId,
                    KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : pedidoId,
                    "mensaje", "Stock insuficiente detectado al iniciar preparación"
            ));
        }
        pedidoRepository.save(pedido);

        sseEmitterManager.publicarTenant(empresaId, "EN_PREPARACION", Map.of(
                KEY_PEDIDO_ID, pedidoId,
                "estado", "EN_PREPARACION"
        ));
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con ID " + pedidoId + " no encontrado"));

        List<PedidoDetalle> itemsEnPreparacion = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION)
                .collect(Collectors.toList());

        if (itemsEnPreparacion.isEmpty()) {
            throw new ReglaNegocioException("No hay ítems en preparación para marcar como listos.");
        }

        itemsEnPreparacion.forEach(d -> d.setEstadoItem(EstadoItem.LISTO));

        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));
        pedidoRepository.save(pedido);

        Long empresaId = TenantContext.getCurrentTenant();
        Long mozoId = pedido.getMozo().getId();
        String mesa = pedido.getIdentificadorMesaReferencia() != null
                ? pedido.getIdentificadorMesaReferencia() : "";
        Map<String, Object> payload = Map.of(
                KEY_PEDIDO_ID, pedidoId,
                KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : pedidoId,
                "mesa", mesa,
                "tipoConsumo", pedido.getTipoConsumo() != null ? pedido.getTipoConsumo() : "",
                "estado", "LISTO"
        );

        // t=0: notificar directamente al mozo creador de la comanda (R2 plano horizontal nivel 0)
        sseEmitterManager.publicarUsuario(empresaId, mozoId, "PEDIDO_LISTO", payload);
        // t=0: notificar a la pantalla de cocina para que actualice su vista
        sseEmitterManager.publicarPorRol(empresaId, "ROLE_COCINA", "PEDIDO_LISTO", payload);
        // La escalación de t=1min, t=2min, t=5min la gestiona EscalacionScheduler server-side (R2-7)
    }

    private static EstadoPedido calcularEstadoAgregado(List<PedidoDetalle> detalles) {
        List<PedidoDetalle> activos = detalles.stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .collect(Collectors.toList());
        if (activos.isEmpty()) return EstadoPedido.CANCELADO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION)) return EstadoPedido.EN_PREPARACION;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.PENDIENTE))      return EstadoPedido.RECIBIDO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.LISTO))          return EstadoPedido.LISTO;
        return EstadoPedido.ENTREGADO;
    }
}
