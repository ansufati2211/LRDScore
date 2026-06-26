package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.repositories.VwKdsCocinaRepository;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KdsServiceImpl implements IKdsService {

    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;
    private final SseEmitterManager sseEmitterManager;

    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository,
                          PedidoRepository pedidoRepository,
                          SseEmitterManager sseEmitterManager) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
        this.sseEmitterManager = sseEmitterManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        return kdsRepository.findAll();
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        pedidoRepository.iniciarPreparacionYDescontarStock(pedidoId, usuarioId);
        // Notificar a la cocina y sala que el plato está en preparación
        sseEmitterManager.publicar("EN_PREPARACION", Map.of(
                "pedidoId", pedidoId,
                "estado", "EN_PREPARACION"
        ));
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con ID " + pedidoId + " no encontrado"));

        if (pedido.getEstadoActual() != EstadoPedido.EN_PREPARACION) {
            throw new ReglaNegocioException("Solo se puede marcar como LISTO un pedido que está EN_PREPARACION.");
        }

        pedido.setEstadoActual(EstadoPedido.LISTO);
        pedidoRepository.save(pedido);

        // Notificar al mozo que su pedido está listo para retirar
        sseEmitterManager.publicar("PEDIDO_LISTO", Map.of(
                "pedidoId", pedidoId,
                "mesa", pedido.getIdentificadorMesaReferencia() != null
                        ? pedido.getIdentificadorMesaReferencia() : "",
                "estado", "LISTO"
        ));
    }
}
