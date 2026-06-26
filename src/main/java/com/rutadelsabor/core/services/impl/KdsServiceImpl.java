package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.repositories.VwKdsCocinaRepository;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KdsServiceImpl implements IKdsService {

    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;

    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository, PedidoRepository pedidoRepository) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        return kdsRepository.findAll(); // La vista ya filtra solo RECIBIDO y EN_PREPARACION
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        // Ejecuta el SP "sp_iniciar_preparacion" que descuenta stock en cascada según la receta
        pedidoRepository.iniciarPreparacionYDescontarStock(pedidoId, usuarioId);
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido con ID " + pedidoId + " no encontrado"));
        
        pedido.setEstadoActual(EstadoPedido.LISTO);
        pedidoRepository.save(pedido);
    }
}