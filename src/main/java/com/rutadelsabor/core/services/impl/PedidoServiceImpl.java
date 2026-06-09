package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository, 
                             PedidoDetalleRepository detalleRepository, 
                             ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
    }

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

        for (PedidoRequestDTO.PedidoItemDTO item : dto.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setPedido(pedidoGuardado);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecioVenta()); 
            detalle.setSubtotal(item.getSubtotal());
            
            // Guardamos la nota para el KDS
            detalle.setNotasPreparacion(item.getNotasPreparacion());
            
            detalleRepository.save(detalle);
        }
        return pedidoGuardado;
    }

    @Override
    @Transactional
    public void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con ID: " + pedidoId));
        
        if (pedido.getEstadoActual() != EstadoPedido.LISTO && pedido.getEstadoActual() != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("El pedido debe estar LISTO o ENTREGADO para procesar el pago.");
        }

        pedidoRepository.procesarPagoYDescontarStock(
                pedidoId,
                cajeroId,
                pagoDTO.getSesionCajaId(),
                pagoDTO.getMetodoPago(), 
                pagoDTO.getMonto(), 
                pagoDTO.getNumeroYape(),
                pagoDTO.getUltimosDigitos(),
                pagoDTO.getTitular()
        );

        pedido.setEstadoActual(EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con el ID: " + id));
    }

    @Override
    @Transactional
    public void cancelarPedido(Long id) {
        Pedido p = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se pudo cancelar. Pedido no encontrado con el ID: " + id));
                
        p.setEstadoActual(EstadoPedido.CANCELADO);
        pedidoRepository.save(p);
    }

    // NUEVO MÉTODO IMPLEMENTADO PARA EL PUNTO DE VENTA (POS)
    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosActivos() {
        // Excluimos BORRADOR, PAGADO y CANCELADO para traer solo los que están "vivos"
        List<EstadoPedido> estadosActivos = java.util.Arrays.asList(
                EstadoPedido.RECIBIDO, 
                EstadoPedido.EN_PREPARACION, 
                EstadoPedido.LISTO, 
                EstadoPedido.ENTREGADO
        );
        return pedidoRepository.findByEstadoActualInOrderByCreatedAtDesc(estadosActivos);
    }
}