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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Service
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final CajaRepository cajaRepository;
    private final SseEmitterManager sseEmitterManager;
    private final IInventarioService inventarioService;
    private final DocumentoCobroRepository documentoCobroRepository;
    private final SedeRepository sedeRepository; 

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PedidoDetalleRepository detalleRepository,
                             ProductoRepository productoRepository,
                             CajaRepository cajaRepository,
                             SseEmitterManager sseEmitterManager,
                             IInventarioService inventarioService,
                             DocumentoCobroRepository documentoCobroRepository,
                             SedeRepository sedeRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.cajaRepository = cajaRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.inventarioService = inventarioService;
        this.documentoCobroRepository = documentoCobroRepository;
        this.sedeRepository = sedeRepository;
    }

    private void validarSedeDeEmpresa(Long sedeIdFiltro) {
        Sede sede = sedeRepository.findById(sedeIdFiltro)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada con ID: " + sedeIdFiltro));
        if (!sede.getEmpresaId().equals(TenantContext.getCurrentTenant())) {
            throw new ReglaNegocioException("La sede indicada no pertenece a su empresa.");
        }
    }

    @Override
    @Transactional
    public Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo) {
        Pedido pedido = new Pedido();
        pedido.setSedeId(TenantContext.resolverSedeEfectiva(dto.getSedeId()));
        pedido.setMozo(mozo);
        pedido.setTipoConsumo(dto.getTipoConsumo());
        pedido.setIdentificadorMesaReferencia(dto.getMesa());
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

            BigDecimal subtotal = producto.getPrecioVenta().multiply(new BigDecimal(item.getCantidad()));
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setSubtotal(subtotal);
            detalle.setNotasPreparacion(item.getNotasPreparacion());
            detalle.setEstadoItem(EstadoItem.PENDIENTE);
            detalle.setNumeroComanda(1);

            detalleRepository.save(detalle);
            totalCalculado = totalCalculado.add(subtotal);
        }

        pedidoGuardado.setNumeroOrden(Math.toIntExact(pedidoGuardado.getId()));
        pedidoGuardado.setSubtotal(totalCalculado);
        pedidoGuardado.setTotal(totalCalculado);
        return pedidoRepository.save(pedidoGuardado);
    }

    @Override
    @Transactional
    public Pedido confirmarPedido(Long id) {
        Pedido pedido = obtenerPedidoInterno(id);
        if (pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("Solo un pedido en BORRADOR puede ser confirmado hacia la cocina.");
        }

        List<String> agotados = pedido.getDetalles().stream()
                .map(PedidoDetalle::getProducto)
                .filter(p -> p.getEstadoDisponibilidad() != EstadoDisponibilidad.DISPONIBLE)
                .map(Producto::getNombre)
                .toList();
        if (!agotados.isEmpty()) {
            throw new ReglaNegocioException(
                    "El pedido contiene producto(s) agotado(s) y no puede confirmarse: " + String.join(", ", agotados));
        }

        inventarioService.reservarInsumosParaPedido(id, pedido.getDetalles());

        pedido.setEstadoActual(EstadoPedido.RECIBIDO);
        Long empresaId = TenantContext.getCurrentTenant();
        
        String mesaSegura = pedido.getIdentificadorMesaReferencia();
        if (mesaSegura == null || mesaSegura.trim().isEmpty()) {
            mesaSegura = "Barra";
        }
        
        sseEmitterManager.publicarTenant(empresaId, "NUEVO_PEDIDO", Map.of(
                "pedidoId", id,
                "mesa", mesaSegura,
                "estado", "RECIBIDO"
        ));
        return pedido;
    }

    @Override
    @Transactional
    public Pedido entregarPedido(Long id) {
        Pedido pedido = obtenerPedidoInterno(id);

        // 🔥 Se elimina la restricción estricta. El mozo ahora puede marcar el pedido 
        // como ENTREGADO directamente, forzando también el estado de sus detalles.
        
        pedido.setEstadoActual(EstadoPedido.ENTREGADO);

        pedido.getDetalles().forEach(detalle -> {
            if (detalle.getEstadoItem() != EstadoItem.CANCELADO) {
                detalle.setEstadoItem(EstadoItem.ENTREGADO);
            }
        });

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId) {
        Pedido pedido = obtenerPedidoInterno(pedidoId);

        if (pedido.getEstadoActual() != EstadoPedido.LISTO && pedido.getEstadoActual() != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("El pedido debe estar LISTO o ENTREGADO para procesar el pago.");
        }

        SesionCaja sesionCaja = cajaRepository.findById(pagoDTO.getSesionCajaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de caja no encontrada con ID: " + pagoDTO.getSesionCajaId()));

        BigDecimal sumaPagos = BigDecimal.ZERO;
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

        if (sumaPagos.compareTo(pedido.getTotal()) < 0) {
            throw new ReglaNegocioException("El monto entregado es inferior al total del pedido. Faltan S/ " + pedido.getTotal().subtract(sumaPagos));
        }

        pedido.setSesionCaja(sesionCaja);
        pedido.setEstadoActual(EstadoPedido.PAGADO);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedido(Long id) {
        return obtenerPedidoInterno(id);
    }

    @Override
    @Transactional
    public Pedido cancelarPedido(Long id) {
        Pedido p = obtenerPedidoInterno(id);
        if (p.getEstadoActual() == EstadoPedido.PAGADO || p.getEstadoActual() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se puede cancelar un pedido " + p.getEstadoActual() + ".");
        }

        if (p.getEstadoActual() != EstadoPedido.BORRADOR) {
            List<PedidoDetalle> pendientes = p.getDetalles().stream()
                    .filter(d -> d.getEstadoItem() == EstadoItem.PENDIENTE || d.getEstadoItem() == EstadoItem.EN_PREPARACION)
                    .toList();
            if (!pendientes.isEmpty()) {
                inventarioService.liberarReservaDeItems(id, pendientes);
            }
        }

        p.getDetalles().stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .forEach(d -> d.setEstadoItem(EstadoItem.CANCELADO));

        p.setEstadoActual(EstadoPedido.CANCELADO);
        return pedidoRepository.save(p);
    }

    @Override
    @Transactional
    public void cancelarItem(Long pedidoId, Long detalleId, String motivo, boolean esGerente) {
        Pedido pedido = obtenerPedidoInterno(pedidoId);

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

        if (detalle.getEstadoItem() == EstadoItem.ENTREGADO) {
            if (!esGerente) {
                throw new ReglaNegocioException("Cancelar un ítem ya ENTREGADO requiere rol GERENTE o ADMIN.");
            }
            if (motivo == null || motivo.isBlank()) {
                throw new ReglaNegocioException("Se requiere un motivo para cancelar un ítem ENTREGADO (auditoría).");
            }
        }

        if (detalle.getEstadoItem() == EstadoItem.PENDIENTE && pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            inventarioService.liberarReservaDeItems(pedidoId, List.of(detalle));
        }

        detalle.setEstadoItem(EstadoItem.CANCELADO);
        detalle.setMotivoCancelacion(motivo);

        BigDecimal nuevoSubtotal = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .map(PedidoDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        pedido.setSubtotal(nuevoSubtotal);
        
        BigDecimal descuento = pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO;
        BigDecimal nuevoTotal = nuevoSubtotal.subtract(descuento);
        if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) {
            nuevoTotal = BigDecimal.ZERO;
        }
        pedido.setTotal(nuevoTotal);

        EstadoPedido nuevoEstado = calcularEstadoAgregado(pedido.getDetalles());
        if (pedido.getEstadoActual() == EstadoPedido.BORRADOR && nuevoEstado == EstadoPedido.RECIBIDO) {
            nuevoEstado = EstadoPedido.BORRADOR;
        }
        
        pedido.setEstadoActual(nuevoEstado);
        pedidoRepository.save(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoActivoResponseDTO> listarPedidosActivos(Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        
        List<EstadoPedido> estados = Arrays.asList(
                EstadoPedido.BORRADOR, 
                EstadoPedido.RECIBIDO, 
                EstadoPedido.EN_PREPARACION, 
                EstadoPedido.LISTO, 
                EstadoPedido.ENTREGADO
        );
        
        List<Pedido> pedidos;
        if (sedeId != null) {
            pedidos = pedidoRepository.findBySedeIdAndEstadoActualInOrderByCreatedAtDesc(sedeId, estados);
        } else {
            if (sedeIdFiltro != null) {
                validarSedeDeEmpresa(sedeIdFiltro);
                pedidos = pedidoRepository.findBySedeIdAndEstadoActualInOrderByCreatedAtDesc(sedeIdFiltro, estados);
            } else {
                pedidos = pedidoRepository.findByEstadoActualInOrderByCreatedAtDesc(estados);
            }
        }
        return pedidos.stream().map(this::mapToActivoResponseDTO).toList();
    }

    @Override
    @Transactional
    public Pedido aplicarDescuento(Long id, BigDecimal descuento) {
        Pedido pedido = obtenerPedidoInterno(id);
        if (pedido.getEstadoActual() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("El descuento solo puede aplicarse a pedidos en estado BORRADOR.");
        }
        if (descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(pedido.getSubtotal()) > 0) {
            throw new ReglaNegocioException("El descuento debe ser mayor a 0 y no puede superar el subtotal del pedido.");
        }
        pedido.setDescuento(descuento);
        pedido.setTotal(pedido.getSubtotal().subtract(descuento));
        return pedido;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoActivoResponseDTO> listarHistorial(LocalDate inicio, LocalDate fin, Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        List<EstadoPedido> hist = Arrays.asList(EstadoPedido.PAGADO, EstadoPedido.CANCELADO);
        List<Pedido> pedidos;

        if (sedeId != null) {
            pedidos = pedidoRepository.findBySedeIdAndEstadoActualInAndCreatedAtBetweenOrderByCreatedAtDesc(
                    sedeId, hist, inicio.atStartOfDay(), fin.atTime(23, 59, 59));
        } else {
            if (sedeIdFiltro != null) {
                validarSedeDeEmpresa(sedeIdFiltro);
                pedidos = pedidoRepository.findBySedeIdAndEstadoActualInAndCreatedAtBetweenOrderByCreatedAtDesc(
                        sedeIdFiltro, hist, inicio.atStartOfDay(), fin.atTime(23, 59, 59));
            } else {
                pedidos = pedidoRepository.findByEstadoActualInAndCreatedAtBetweenOrderByCreatedAtDesc(
                        hist, inicio.atStartOfDay(), fin.atTime(23, 59, 59));
            }
        }
        return pedidos.stream().map(this::mapToActivoResponseDTO).toList();
    }

    // --- MÓDULO 4 ---

    @Override
    @Transactional
    public void agregarItemsAPedido(Long pedidoId, AgregarItemsRequestDTO dto) {
        Pedido pedido = obtenerPedidoInterno(pedidoId);

        EstadoPedido estado = pedido.getEstadoActual();
        if (estado == EstadoPedido.BORRADOR || estado == EstadoPedido.PAGADO || estado == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se pueden agregar ítems a un pedido en estado " + estado + ".");
        }

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
        }).toList();

        inventarioService.reservarInsumosParaPedido(pedidoId, nuevosDetalles);

        BigDecimal incremento = nuevosDetalles.stream()
                .map(PedidoDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setSubtotal(pedido.getSubtotal().add(incremento));
        pedido.setTotal(pedido.getSubtotal().subtract(
                pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO));

        if (estado == EstadoPedido.LISTO || estado == EstadoPedido.ENTREGADO) {
            pedido.setEstadoActual(EstadoPedido.RECIBIDO);
        }
        pedidoRepository.save(pedido);

        Long empresaId = TenantContext.getCurrentTenant();
        sseEmitterManager.publicarTenant(empresaId, "NUEVA_COMANDA", Map.of(
                "pedidoId", pedidoId,
                "numeroComanda", nuevaComanda,
                "items", nuevosDetalles.stream().map(d -> Map.of(
                        "detalleId", d.getId(),
                        "producto", d.getProducto().getNombre(),
                        "cantidad", d.getCantidad()
                )).toList()
        ));
    }

    @Override
    @Transactional
    public DocumentoCobroResponseDTO crearDocumentoCobro(Long pedidoId, DocumentoCobroRequestDTO dto) {
        Pedido pedido = obtenerPedidoInterno(pedidoId);

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
                    .toList();
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

        if (sumaPagos.compareTo(doc.getTotal()) < 0) {
            throw new ReglaNegocioException("El monto entregado no cubre el total del documento. Faltan S/ "
                    + doc.getTotal().subtract(sumaPagos));
        }

        doc.setEstado("PAGADO");
        pedido.setSesionCaja(sesionCaja);
        documentoCobroRepository.save(doc);

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
                .toList();
    }

    private static EstadoPedido calcularEstadoAgregado(List<PedidoDetalle> detalles) {
        List<PedidoDetalle> activos = detalles.stream()
                .filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO)
                .toList();
        if (activos.isEmpty()) return EstadoPedido.CANCELADO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION)) return EstadoPedido.EN_PREPARACION;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.PENDIENTE))      return EstadoPedido.RECIBIDO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.LISTO))          return EstadoPedido.LISTO;
        return EstadoPedido.ENTREGADO;
    }
    
    private Pedido obtenerPedidoInterno(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con ID: " + id));
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
        }).toList();

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
                .toList());
        return dto;
    }

    
}