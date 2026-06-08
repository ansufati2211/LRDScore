package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoServiceImpl implements IPedidoService {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoDetalleRepository detalleRepository;
    @Autowired private ProductoRepository productoRepository;

    @Override
    @Transactional
    public Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo) {
        Pedido pedido = new Pedido();
        pedido.setMozo(mozo);
        pedido.setTipoConsumo(dto.getTipoConsumo());
        pedido.setIdentificadorMesaReferencia(dto.getMesa());
        pedido.setEstadoActual(EstadoPedido.RECIBIDO);
        pedido.setTotal(dto.getTotal());

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
// ... dentro del bucle de items:
for (PedidoRequestDTO.PedidoItemDTO item : dto.getItems()) {
    Producto producto = productoRepository.findById(item.getProductoId())
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

    PedidoDetalle detalle = new PedidoDetalle();
    detalle.setPedido(pedidoGuardado);
    detalle.setProducto(producto);
    detalle.setCantidad(item.getCantidad());
    
    // --- AQUÍ ESTABA EL ERROR: Faltaba asignar el precio unitario ---
    detalle.setPrecioUnitario(producto.getPrecioVenta()); 
    
    // El subtotal ya lo envías en el DTO, así que eso está bien
    detalle.setSubtotal(item.getSubtotal());
    
    detalleRepository.save(detalle);
}
        return pedidoGuardado;
    }

    @Override
    public Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public void cancelarPedido(Long id) {
        Pedido p = obtenerPedido(id);
        p.setEstadoActual(EstadoPedido.CANCELADO);
        pedidoRepository.save(p);
    }
}