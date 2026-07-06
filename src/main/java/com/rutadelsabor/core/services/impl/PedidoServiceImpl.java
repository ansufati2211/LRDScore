package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.AgregarItemsRequestDTO;
import com.rutadelsabor.core.dto.request.DocumentoCobroRequestDTO;
import com.rutadelsabor.core.dto.request.PagoItemDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoCobroResponseDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final CajaRepository cajaRepository;
    private final SseEmitterManager sseEmitterManager;
    private final IInventarioService inventarioService;
    private final DocumentoCobroRepository documentoCobroRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PedidoDetalleRepository detalleRepository,
                             ProductoRepository productoRepository,
                             CajaRepository cajaRepository,
                             SseEmitterManager sseEmitterManager,
                             IInventarioService inventarioService,
                             DocumentoCobroRepository documentoCobroRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.cajaRepository = cajaRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.inventarioService = inventarioService;
        this.documentoCobroRepository = documentoCobroRepository;
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
            detalle.setEstadoItem(EstadoItem.PENDIENTE);
            detalle.setNumeroComanda(1);

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
        if (pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("Solo un pedido en BORRADOR puede ser confirmado hacia la cocina.");
        }

        // E6-1: rechazar confirmación si hay productos agotados en el pedido
        List<String> agotados = pedido.getDetalles().stream()
                .map(PedidoDetalle::getProducto)
                .filter(p -> p.getEstadoDisponibilidad() != EstadoDisponibilidad.DISPONIBLE)
                .map(Producto::getNombre)
                .collect(Collectors.toList());
        if (!agotados.isEmpty()) {
            throw new ReglaNegocioException(
                    "El pedido contiene producto(s) agotado(s) y no puede confirmarse: " + String.join(", ", agotados));
        }

        // R3-1: reservar insumos por receta; lanza StockInsuficienteException con detalle si falta stock
        inventarioService.reservarInsumosParaPedido(id, pedido.getDetalles());

        pedido.setEstadoActual(EstadoPedido.RECIBIDO);
        Long empresaId = TenantContext.getCurrentTenant();
        sseEmitterManager.publicarTenant(empresaId, "NUEVO_PEDIDO", Map.of(
                "pedidoId", id,
                "mesa", pedido.getIdentificadorMesaReferencia() != null
                        ? pedido.getIdentificadorMesaReferencia() : "",
                "estado", "RECIBIDO"
        ));
    }

    @Override
    @Transactional
    public void entregarPedido(Long id) {
        Pedido pedido = obtenerPedido(id);

        List<PedidoDetalle> activos = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .collect(Collectors.toList());

        boolean todosListos = activos.stream()
                .allMatch(d -> d.getEstadoItem() == EstadoItem.LISTO
                            || d.getEstadoItem() == EstadoItem.ENTREGADO);
        if (!todosListos) {
            throw new ReglaNegocioException("El pedido aún no está LISTO en la cocina.");
        }

        activos.stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.LISTO)
                .forEach(d -> d.setEstadoItem(EstadoItem.ENTREGADO));

        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));
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
        if (p.getEstadoActual() == EstadoPedido.PAGADO) {
            throw new ReglaNegocioException("No se puede cancelar un pedido ya PAGADO.");
        }

        // Liberar reservas solo de ítems aún PENDIENTE (recipe-based — seguro para multi-comanda)
        List<PedidoDetalle> pendientes = p.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.PENDIENTE)
                .collect(Collectors.toList());
        if (!pendientes.isEmpty()) {
            inventarioService.liberarReservaDeItems(id, pendientes);
        }

        // EN_PREPARACION / LISTO / ENTREGADO → consumo ya registrado, merma implícita
        p.getDetalles().stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .forEach(d -> d.setEstadoItem(EstadoItem.CANCELADO));

        p.setEstadoActual(EstadoPedido.CANCELADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoActivoResponseDTO> listarPedidosActivos() {
        List<EstadoPedido> estadosActivos = Arrays.asList(
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

    @Override
    @Transactional(readOnly = true)
    public List<PedidoActivoResponseDTO> listarHistorial(LocalDate inicio, LocalDate fin) {
        List<EstadoPedido> estadosHistoricos = Arrays.asList(EstadoPedido.PAGADO, EstadoPedido.CANCELADO);
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);
        return pedidoRepository.findByEstadoActualInAndCreatedAtBetweenOrderByCreatedAtDesc(
                estadosHistoricos, inicioDateTime, finDateTime)
                .stream()
                .map(this::mapToActivoResponseDTO)
                .collect(Collectors.toList());
    }

    // --- MÓDULO 4 ---

    @Override
    @Transactional
    public void agregarItemsAPedido(Long pedidoId, AgregarItemsRequestDTO dto) {
        Pedido pedido = obtenerPedido(pedidoId);

        EstadoPedido estado = pedido.getEstadoActual();
        if (estado == EstadoPedido.BORRADOR || estado == EstadoPedido.PAGADO || estado == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se pueden agregar ítems a un pedido en estado " + estado + ".");
        }

        // Determinar el número de comanda siguiente
        int nuevaComanda = pedido.getDetalles().stream()
                .mapToInt(d -> d.getNumeroComanda() != null ? d.getNumeroComanda() : 1)
                .max()
                .orElse(0) + 1;

        List<PedidoDetalle> nuevosDetalles = dto.getItems().stream().map(item -> {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + item.getProductoId()));

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());

            BigDecimal subtotal = producto.getPrecioVenta().multiply(new BigDecimal(item.getCantidad()));
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setSubtotal(subtotal);
            detalle.setNotasPreparacion(item.getNotasPreparacion());
            detalle.setEstadoItem(EstadoItem.PENDIENTE);
            detalle.setNumeroComanda(nuevaComanda);

            return detalleRepository.save(detalle);
        }).collect(Collectors.toList());

        // R4-2: reservar inventario solo de los nuevos ítems
        inventarioService.reservarInsumosParaPedido(pedidoId, nuevosDetalles);

        // Actualizar totales del pedido
        BigDecimal incremento = nuevosDetalles.stream()
                .map(PedidoDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setSubtotal(pedido.getSubtotal().add(incremento));
        pedido.setTotal(pedido.getSubtotal().subtract(
                pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO));

        // E4-2: con nuevos ítems PENDIENTE, el pedido retrocede si estaba en estado final de cocina
        if (estado == EstadoPedido.LISTO || estado == EstadoPedido.ENTREGADO) {
            pedido.setEstadoActual(EstadoPedido.RECIBIDO);
        }
        // RECIBIDO y EN_PREPARACION no cambian — ya tienen ítems activos pendientes
        pedidoRepository.save(pedido);

        // R4-2: notificar a cocina solo los ítems nuevos
        Long empresaId = TenantContext.getCurrentTenant();
        sseEmitterManager.publicarTenant(empresaId, "NUEVA_COMANDA", Map.of(
                "pedidoId", pedidoId,
                "numeroComanda", nuevaComanda,
                "items", nuevosDetalles.stream().map(d -> Map.of(
                        "detalleId", d.getId(),
                        "producto", d.getProducto().getNombre(),
                        "cantidad", d.getCantidad()
                )).collect(Collectors.toList())
        ));
    }

    @Override
    @Transactional
    public void cancelarItem(Long pedidoId, Long detalleId, String motivo, boolean esGerente) {
        Pedido pedido = obtenerPedido(pedidoId);

        if (pedido.getEstadoActual() == EstadoPedido.PAGADO || pedido.getEstadoActual() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se puede cancelar ítems de un pedido " + pedido.getEstadoActual() + ".");
        }

        PedidoDetalle detalle = pedido.getDetalles().stream()
                .filter(d -> d.getId().equals(detalleId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado en el pedido"));

        if (detalle.getEstadoItem() == EstadoItem.CANCELADO) {
            throw new ReglaNegocioException("El ítem ya está cancelado.");
        }

        // E4-1: cancelar un ítem ENTREGADO requiere GERENTE y motivo obligatorio
        if (detalle.getEstadoItem() == EstadoItem.ENTREGADO) {
            if (!esGerente) {
                throw new ReglaNegocioException("Cancelar un ítem ya ENTREGADO requiere rol GERENTE.");
            }
            if (motivo == null || motivo.isBlank()) {
                throw new ReglaNegocioException("Se requiere un motivo para cancelar un ítem ENTREGADO (auditoría).");
            }
        }

        // R4-6: lógica de inventario según estado del ítem
        if (detalle.getEstadoItem() == EstadoItem.PENDIENTE) {
            // Solo reservado → liberar reserva recipe-based
            inventarioService.liberarReservaDeItems(pedidoId, List.of(detalle));
        }
        // EN_PREPARACION / LISTO / ENTREGADO → CONSUMO_PRODUCCION ya registrado, merma implícita

        detalle.setEstadoItem(EstadoItem.CANCELADO);
        detalle.setMotivoCancelacion(motivo);

        // Recalcular subtotal/total excluyendo el ítem recién cancelado — sin esto, el pedido
        // queda cobrando (directo o por split) el monto de ítems que ya no están en la cuenta.
        BigDecimal nuevoSubtotal = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .map(PedidoDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setSubtotal(nuevoSubtotal);
        pedido.setTotal(nuevoSubtotal.subtract(
                pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO));

        // R4-7: recalcular estado agregado
        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));
        pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public DocumentoCobroResponseDTO crearDocumentoCobro(Long pedidoId, DocumentoCobroRequestDTO dto) {
        Pedido pedido = obtenerPedido(pedidoId);

        DocumentoCobro doc = new DocumentoCobro();
        doc.setPedido(pedido);
        doc.setTipo(dto.getTipo());

        if ("ITEMS".equals(dto.getTipo())) {
            if (dto.getDetalleIds() == null || dto.getDetalleIds().isEmpty()) {
                throw new ReglaNegocioException("Debe especificar al menos un ítem para el split por ítems.");
            }
            List<PedidoDetalle> seleccionados = pedido.getDetalles().stream()
                    .filter(d -> dto.getDetalleIds().contains(d.getId())
                              && d.getEstadoItem() != EstadoItem.CANCELADO)
                    .collect(Collectors.toList());
            BigDecimal subtotal = seleccionados.stream()
                    .map(PedidoDetalle::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            doc.setDetalles(seleccionados);
            doc.setSubtotal(subtotal);
            doc.setTotal(subtotal);
        } else if ("MONTO".equals(dto.getTipo())) {
            if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("Debe especificar un monto válido para el split por monto.");
            }
            doc.setMonto(dto.getMonto());
            doc.setSubtotal(dto.getMonto());
            doc.setTotal(dto.getMonto());
        } else {
            throw new ReglaNegocioException("Tipo de documento inválido. Use 'ITEMS' o 'MONTO'.");
        }

        doc.setEstado("PENDIENTE");
        return mapToDocumentoCobroResponseDTO(documentoCobroRepository.save(doc));
    }

    @Override
    @Transactional
    public DocumentoCobroResponseDTO pagarDocumentoCobro(Long documentoId, PagoRequestDTO pagoDTO, Long cajeroId) {
        DocumentoCobro doc = documentoCobroRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento de cobro no encontrado con ID: " + documentoId));

        if (!"PENDIENTE".equals(doc.getEstado())) {
            throw new ReglaNegocioException("El documento de cobro ya fue pagado.");
        }

        Pedido pedido = doc.getPedido();
        if (pedido.getEstadoActual() != EstadoPedido.LISTO && pedido.getEstadoActual() != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("El pedido debe estar LISTO o ENTREGADO para procesar el pago.");
        }

        SesionCaja sesionCaja = cajaRepository.findById(pagoDTO.getSesionCajaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de caja no encontrada con ID: " + pagoDTO.getSesionCajaId()));

        BigDecimal sumaPagos = BigDecimal.ZERO;
        for (PagoItemDTO item : pagoDTO.getPagos()) {
            pedidoRepository.registrarPago(
                    pedido.getId(),
                    pagoDTO.getSesionCajaId(),
                    item.getMetodoPago(),
                    item.getMonto(),
                    item.getNumeroYape(),
                    item.getUltimosDigitos(),
                    item.getTitular()
            );
            sumaPagos = sumaPagos.add(item.getMonto());
        }

        // E4-3: el documento no puede cerrarse si el monto entregado no cubre su total
        if (sumaPagos.compareTo(doc.getTotal()) < 0) {
            throw new ReglaNegocioException("El monto entregado no cubre el total del documento. Faltan S/ "
                    + doc.getTotal().subtract(sumaPagos));
        }

        doc.setEstado("PAGADO");
        pedido.setSesionCaja(sesionCaja);
        documentoCobroRepository.save(doc);

        // R4-4: pedido PAGADO solo cuando suma de documentos PAGADO >= total del pedido
        BigDecimal totalPagado = documentoCobroRepository.findByPedidoId(pedido.getId()).stream()
                .filter(d -> "PAGADO".equals(d.getEstado()))
                .map(DocumentoCobro::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(pedido.getTotal()) >= 0) {
            pedido.setEstadoActual(EstadoPedido.PAGADO);
            pedidoRepository.save(pedido);
        }

        return mapToDocumentoCobroResponseDTO(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoCobroResponseDTO> listarDocumentosCobro(Long pedidoId) {
        return documentoCobroRepository.findByPedidoId(pedidoId).stream()
                .map(this::mapToDocumentoCobroResponseDTO)
                .collect(Collectors.toList());
    }

    // --- helpers privados ---

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
        dto.setRequiereRevision(p.getRequiereRevision());

        List<PedidoActivoResponseDTO.DetallePlanoDTO> items = p.getDetalles().stream().map(d -> {
            PedidoActivoResponseDTO.DetallePlanoDTO item = new PedidoActivoResponseDTO.DetallePlanoDTO();
            item.setDetalleId(d.getId());
            item.setProductoId(d.getProducto().getId());
            item.setNombreProducto(d.getProducto().getNombre());
            item.setCantidad(d.getCantidad());
            item.setPrecioUnitario(d.getPrecioUnitario());
            item.setSubtotal(d.getSubtotal());
            item.setNotasPreparacion(d.getNotasPreparacion());
            item.setEstadoItem(d.getEstadoItem() != null ? d.getEstadoItem().name() : EstadoItem.PENDIENTE.name());
            item.setNumeroComanda(d.getNumeroComanda());
            return item;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    private DocumentoCobroResponseDTO mapToDocumentoCobroResponseDTO(DocumentoCobro doc) {
        DocumentoCobroResponseDTO dto = new DocumentoCobroResponseDTO();
        dto.setId(doc.getId());
        dto.setTipo(doc.getTipo());
        dto.setEstado(doc.getEstado());
        dto.setSubtotal(doc.getSubtotal());
        dto.setTotal(doc.getTotal());
        dto.setMonto(doc.getMonto());
        dto.setDetalleIds(doc.getDetalles().stream()
                .map(BaseTenantEntity::getId)
                .collect(Collectors.toList()));
        return dto;
    }
}
