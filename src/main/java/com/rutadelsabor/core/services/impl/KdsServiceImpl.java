package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.repositories.VwKdsCocinaRepository;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import com.rutadelsabor.core.utils.Constantes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class KdsServiceImpl implements IKdsService {

    // 1. Declaramos las dependencias como finales e inmutables (Resuelve java:S6813)
    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;

    // 2. Inyección por Constructor: Garantiza un acoplamiento limpio y testeable
    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository, PedidoRepository pedidoRepository) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        List<String> estadosPendientes = Arrays.asList(
                Constantes.ESTADO_PEDIDO_RECIBIDO, 
                Constantes.ESTADO_PEDIDO_EN_PREPARACION
        );
        return kdsRepository.findByEstadoPedidoInOrderByFechaPedidoAsc(estadosPendientes);
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId) {
        cambiarEstadoPedido(pedidoId, EstadoPedido.EN_PREPARACION);
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        cambiarEstadoPedido(pedidoId, EstadoPedido.LISTO);
    }

    /**
     * Centraliza la lógica de cambio de estado de manera defensiva.
     * Utiliza la excepción de la Fase 1 perfectamente integrada al GlobalExceptionHandler.
     */
    private void cambiarEstadoPedido(Long pedidoId, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con ID " + pedidoId + " no encontrado"));
        
        pedido.setEstadoActual(nuevoEstado);
        pedidoRepository.save(pedido);
    }
}