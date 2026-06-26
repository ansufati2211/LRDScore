package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.PagoItemDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
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
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final CajaRepository cajaRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PedidoDetalleRepository detalleRepository,
                             ProductoRepository productoRepository,
                             CajaRepository cajaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.cajaRepository = cajaRepository;
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

        // El ID de la BD sirve como número de orden (único, sin race conditions)
        pedidoGuardado.setNumeroOrden(Math.toIntExact(pedidoGuardado.getId()));
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

        SesionCaja sesionCaja = cajaRepository.findById(pagoDTO.getSesionCajaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de caja no encontrada con ID: " + pagoDTO.getSesionCajaId()));

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
        if (sumaPagos.compareTo(pedido.getTotal()) < 0) {
            throw new ReglaNegocioException("El monto entregado es inferior al total del pedido. Faltan S/ " + pedido.getTotal().subtract(sumaPagos));
        }

        // Vincular el pedido a la sesión de caja para trazabilidad en reportes
        pedido.setSesionCaja(sesionCaja);
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
        if (p.getEstadoActual() == EstadoPedido.EN_PREPARACION || p.getEstadoActual() == EstadoPedido.LISTO) {
            throw new ReglaNegocioException(
                "No se puede cancelar un pedido en estado " + p.getEstadoActual() +
                ". El stock ya fue descontado del inventario. Registre un ajuste manual en /api/inventario/ajustes."
            );
        }
        if (p.getEstadoActual() == EstadoPedido.PAGADO || p.getEstadoActual() == EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("No se puede cancelar un pedido ya " + p.getEstadoActual() + ".");
        }
        p.setEstadoActual(EstadoPedido.CANCELADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoActivoResponseDTO> listarPedidosActivos() {
        List<EstadoPedido> estadosActivos = java.util.Arrays.asList(
                EstadoPedido.RECIBIDO,
                EstadoPedido.EN_PREPARACION,
                EstadoPedido.LISTO,
                EstadoPedido.ENTREGADO
        );
        return pedidoRepository.findByEstadoActualInOrderByCreatedAtDesc(estadosActivos)
                .stream()
                .map(this::mapToActivoResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void aplicarDescuento(Long id, BigDecimal descuento) {
        Pedido pedido = obtenerPedido(id);
        if (pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("El descuento solo puede aplicarse a pedidos en estado BORRADOR.");
        }
        if (descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(pedido.getSubtotal()) > 0) {
            throw new ReglaNegocioException("El descuento debe ser mayor a 0 y no puede superar el subtotal del pedido.");
        }
        pedido.setDescuento(descuento);
        pedido.setTotal(pedido.getSubtotal().subtract(descuento));
    }

    private PedidoActivoResponseDTO mapToActivoResponseDTO(Pedido p) {
        PedidoActivoResponseDTO dto = new PedidoActivoResponseDTO();
        dto.setId(p.getId());
        dto.setMozo(p.getMozo().getNombre());
        dto.setTipoConsumo(p.getTipoConsumo());
        dto.setMesa(p.getIdentificadorMesaReferencia());
        dto.setEstadoActual(p.getEstadoActual().name());
        dto.setDescuento(p.getDescuento());
        dto.setTotal(p.getTotal());
        dto.setFechaCreacion(p.getCreatedAt());

        List<PedidoActivoResponseDTO.DetallePlanoDTO> items = p.getDetalles().stream().map(d -> {
            PedidoActivoResponseDTO.DetallePlanoDTO item = new PedidoActivoResponseDTO.DetallePlanoDTO();
            item.setProductoId(d.getProducto().getId());
            item.setNombreProducto(d.getProducto().getNombre());
            item.setCantidad(d.getCantidad());
            item.setPrecioUnitario(d.getPrecioUnitario());
            item.setSubtotal(d.getSubtotal());
            item.setNotasPreparacion(d.getNotasPreparacion());
            return item;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
}