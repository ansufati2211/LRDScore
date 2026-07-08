package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KdsServiceImpl implements IKdsService {

    private static final String KEY_PEDIDO_ID = "pedidoId";
    private static final String KEY_NUMERO_ORDEN = "numeroOrden";
    private static final String KEY_ESTADO = "estado";
    private static final String KEY_PRODUCTO_ID = "productoId";
    private static final String KEY_NOMBRE = "nombre";
    private static final String MSG_NO_ENCONTRADO = " no encontrado";
    private static final String PREFIX_PEDIDO = "Pedido con ID ";
    private static final String PREFIX_PRODUCTO = "Producto ";
    private static final String VALOR_EN_PREPARACION = "EN_PREPARACION";

    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final InsumoSedeRepository insumoSedeRepository; // <-- INYECTADO PARA FIX DE STOCK
    private final SseEmitterManager sseEmitterManager;
    private final IInventarioService inventarioService;

    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository, PedidoRepository pedidoRepository,
                          ProductoRepository productoRepository, RecetaDetalleRepository recetaDetalleRepository,
                          InsumoSedeRepository insumoSedeRepository, SseEmitterManager sseEmitterManager,
                          IInventarioService inventarioService) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.insumoSedeRepository = insumoSedeRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        // FIX: La cocina solo ve los pedidos de SU SEDE
        return kdsRepository.findBySedeIdAndEstadoPedidoInOrderByHoraIngresoAsc(
            TenantContext.getCurrentSede(), List.of("RECIBIDO", VALOR_EN_PREPARACION));
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PEDIDO + pedidoId + MSG_NO_ENCONTRADO));

        if (pedido.getEstadoActual() == EstadoPedido.PAGADO || pedido.getEstadoActual() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se puede iniciar preparación en un pedido " + pedido.getEstadoActual());
        }

        List<PedidoDetalle> itemsPendientes = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.PENDIENTE).toList();
        if (itemsPendientes.isEmpty()) throw new ReglaNegocioException("No hay ítems pendientes.");

        Long empresaId = TenantContext.getCurrentTenant();
        boolean requiereRevision = inventarioService.convertirItemsAConsumo(pedidoId, itemsPendientes);

        itemsPendientes.forEach(d -> d.setEstadoItem(EstadoItem.EN_PREPARACION));
        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));

        if (requiereRevision) {
            pedido.setRequiereRevision(true);
            sseEmitterManager.publicarPorRol(empresaId, "ROLE_GERENTE_SEDE", "PEDIDO_REQUIERE_REVISION", Map.of(
                    KEY_PEDIDO_ID, pedidoId, KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : (Object) pedidoId,
                    "mensaje", "Stock insuficiente detectado al iniciar preparación"
            ));
        }
        pedidoRepository.save(pedido);
        sseEmitterManager.publicarTenant(empresaId, VALOR_EN_PREPARACION, Map.of(KEY_PEDIDO_ID, pedidoId, KEY_ESTADO, VALOR_EN_PREPARACION));
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PEDIDO + pedidoId + MSG_NO_ENCONTRADO));
        List<PedidoDetalle> itemsEnPreparacion = pedido.getDetalles().stream().filter(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION).toList();
        if (itemsEnPreparacion.isEmpty()) throw new ReglaNegocioException("No hay ítems en preparación.");

        itemsEnPreparacion.forEach(d -> d.setEstadoItem(EstadoItem.LISTO));
        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));
        pedidoRepository.save(pedido);

        Long empresaId = TenantContext.getCurrentTenant();
        Map<String, Object> payload = Map.of(
                KEY_PEDIDO_ID, pedidoId, KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : (Object) pedidoId,
                "mesa", pedido.getIdentificadorMesaReferencia() != null ? pedido.getIdentificadorMesaReferencia() : "",
                "tipoConsumo", pedido.getTipoConsumo() != null ? pedido.getTipoConsumo() : "", KEY_ESTADO, "LISTO"
        );
        sseEmitterManager.publicarUsuario(empresaId, pedido.getMozo().getId(), "PEDIDO_LISTO", payload);
        sseEmitterManager.publicarPorRol(empresaId, "ROLE_COCINA", "PEDIDO_LISTO", payload);
    }

    @Override
    @Transactional
    public void marcarAgotadoTemporal(Long productoId) {
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_TEMPORAL);
        productoRepository.save(producto);
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_AGOTADO", Map.of(KEY_PRODUCTO_ID, productoId, KEY_NOMBRE, producto.getNombre(), KEY_ESTADO, "AGOTADO_TEMPORAL"));
    }

    @Override
    @Transactional
    public void marcarAgotadoServicio(Long productoId) {
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_SERVICIO);
        productoRepository.save(producto);
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_AGOTADO", Map.of(KEY_PRODUCTO_ID, productoId, KEY_NOMBRE, producto.getNombre(), KEY_ESTADO, "AGOTADO_SERVICIO"));
    }

    @Override
    @Transactional
    public void revertirDisponible(Long productoId) {
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        if (producto.getEstadoDisponibilidad() == EstadoDisponibilidad.AGOTADO_SERVICIO) throw new ReglaNegocioException("AGOTADO_SERVICIO no puede revertirse manualmente.");
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        productoRepository.save(producto);
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_DISPONIBLE", Map.of(KEY_PRODUCTO_ID, productoId, KEY_NOMBRE, producto.getNombre()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorcionDisponibleDTO> calcularPorcionesDisponibles() {
        List<Producto> productos = productoRepository.findByEstadoRegistroTrue();
        List<PorcionDisponibleDTO> resultado = new ArrayList<>();
        Long sedeId = TenantContext.getCurrentSede(); // MULTI-SEDE

        for (Producto producto : productos) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(producto.getId());
            PorcionDisponibleDTO dto = new PorcionDisponibleDTO();
            dto.setProductoId(producto.getId());
            dto.setProducto(producto.getNombre());
            dto.setEstadoDisponibilidad(producto.getEstadoDisponibilidad().name());
            dto.setPorcionesDisponibles(calcularMinimoPorciones(receta, sedeId)); // FIX
            resultado.add(dto);
        }
        return resultado;
    }

    // FIX: Ahora extrae el stock de la tabla InsumoSede
    private BigDecimal calcularMinimoPorciones(List<RecetaDetalle> receta, Long sedeId) {
        if (receta == null || receta.isEmpty()) return null;

        return receta.stream()
                .filter(rd -> rd.getCantidadRequerida().compareTo(BigDecimal.ZERO) > 0)
                .map(rd -> {
                    InsumoSede is = insumoSedeRepository.findBySedeIdAndInsumoId(sedeId, rd.getInsumo().getId()).orElse(null);
                    if (is == null) return BigDecimal.ZERO;
                    BigDecimal stockDisponible = is.getStockActual().subtract(is.getStockReservado());
                    return stockDisponible.divide(rd.getCantidadRequerida(), 2, RoundingMode.FLOOR);
                })
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private static EstadoPedido calcularEstadoAgregado(List<PedidoDetalle> detalles) {
        List<PedidoDetalle> activos = detalles.stream().filter(d -> d.getEstadoItem() != EstadoItem.CANCELADO).toList();
        if (activos.isEmpty()) return EstadoPedido.CANCELADO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION)) return EstadoPedido.EN_PREPARACION;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.PENDIENTE)) return EstadoPedido.RECIBIDO;
        if (activos.stream().anyMatch(d -> d.getEstadoItem() == EstadoItem.LISTO)) return EstadoPedido.LISTO;
        return EstadoPedido.ENTREGADO;
    }
}