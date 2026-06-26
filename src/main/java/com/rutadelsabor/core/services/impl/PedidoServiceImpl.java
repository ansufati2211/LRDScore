package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.PagoItemDTO;
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

import java.math.BigDecimal;
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
        
        // El pedido nace en BORRADOR (El mozo aún está tomando la orden)
        pedido.setEstadoActual(EstadoPedido.BORRADOR);
        pedido.setNotasGenerales(dto.getNotasGenerales());

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        BigDecimal totalCalculado = BigDecimal.ZERO;

        for (PedidoRequestDTO.PedidoItemDTO item : dto.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setPedido(pedidoGuardado);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            
            // SEGURIDAD FINANCIERA: El backend asume el control total de los precios
            BigDecimal subtotal = producto.getPrecioVenta().multiply(new BigDecimal(item.getCantidad()));
            detalle.setPrecioUnitario(producto.getPrecioVenta()); 
            detalle.setSubtotal(subtotal);
            detalle.setNotasPreparacion(item.getNotasPreparacion());
            
            detalleRepository.save(detalle);
            totalCalculado = totalCalculado.add(subtotal);
        }
        
        // Asignamos el total auditable y guardamos
        pedidoGuardado.setSubtotal(totalCalculado);
        pedidoGuardado.setTotal(totalCalculado);
        return pedidoRepository.save(pedidoGuardado);
    }

    @Override
    @Transactional
    public void confirmarPedido(Long id) {
        Pedido pedido = obtenerPedido(id);
        if(pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("Solo un pedido en BORRADOR puede ser confirmado hacia la cocina.");
        }
        pedido.setEstadoActual(EstadoPedido.RECIBIDO);
    }

    @Override
    @Transactional
    public void entregarPedido(Long id) {
        Pedido pedido = obtenerPedido(id);
        if(pedido.getEstadoActual() != EstadoPedido.LISTO) {
            throw new ReglaNegocioException("El pedido aún no está LISTO en la cocina.");
        }
        pedido.setEstadoActual(EstadoPedido.ENTREGADO);
    }

    @Override
    @Transactional
    public void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId) {
        Pedido pedido = obtenerPedido(pedidoId);
        
        if (pedido.getEstadoActual() != EstadoPedido.LISTO && pedido.getEstadoActual() != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("El pedido debe estar LISTO o ENTREGADO para procesar el pago.");
        }

        BigDecimal sumaPagos = BigDecimal.ZERO;

        // PAGO MIXTO: Iteramos sobre los múltiples métodos con los que el cliente pudo haber pagado
        for (PagoItemDTO item : pagoDTO.getPagos()) {
            pedidoRepository.registrarPago(
                    pedidoId,
                    pagoDTO.getSesionCajaId(),
                    item.getMetodoPago(), 
                    item.getMonto(), 
                    item.getNumeroYape(),
                    item.getUltimosDigitos(),
                    item.getTitular()
            );
            sumaPagos = sumaPagos.add(item.getMonto());
        }

        // Validación estricta de cuadre financiero
        if(sumaPagos.compareTo(pedido.getTotal()) < 0) {
             throw new ReglaNegocioException("El monto entregado es inferior al total del pedido. Faltan S/ " + pedido.getTotal().subtract(sumaPagos));
        }

        pedido.setEstadoActual(EstadoPedido.PAGADO);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void cancelarPedido(Long id) {
        Pedido p = obtenerPedido(id);
        p.setEstadoActual(EstadoPedido.CANCELADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosActivos() {
        List<EstadoPedido> estadosActivos = java.util.Arrays.asList(
                EstadoPedido.RECIBIDO, 
                EstadoPedido.EN_PREPARACION, 
                EstadoPedido.LISTO, 
                EstadoPedido.ENTREGADO
        );
        return pedidoRepository.findByEstadoActualInOrderByCreatedAtDesc(estadosActivos);
    }
}