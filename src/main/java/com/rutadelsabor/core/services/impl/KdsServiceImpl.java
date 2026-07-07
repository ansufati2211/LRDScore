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
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.repositories.ProductoRepository;
import com.rutadelsabor.core.repositories.RecetaDetalleRepository;
import com.rutadelsabor.core.repositories.VwKdsCocinaRepository;
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

    private final VwKdsCocinaRepository kdsRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final SseEmitterManager sseEmitterManager;
    private final IInventarioService inventarioService;

    public KdsServiceImpl(VwKdsCocinaRepository kdsRepository,
                          PedidoRepository pedidoRepository,
                          ProductoRepository productoRepository,
                          RecetaDetalleRepository recetaDetalleRepository,
                          SseEmitterManager sseEmitterManager,
                          IInventarioService inventarioService) {
        this.kdsRepository = kdsRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VwKdsCocina> obtenerPedidosPendientes() {
        return kdsRepository.findAll();
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PEDIDO + pedidoId + MSG_NO_ENCONTRADO));

        if (pedido.getEstadoActual() == EstadoPedido.PAGADO || pedido.getEstadoActual() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("No se puede iniciar preparación en un pedido " + pedido.getEstadoActual() + ".");
        }

        List<PedidoDetalle> itemsPendientes = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.PENDIENTE)
            .toList();

        if (itemsPendientes.isEmpty()) {
            throw new ReglaNegocioException("No hay ítems pendientes para iniciar preparación.");
        }

        Long empresaId = TenantContext.getCurrentTenant();

        boolean requiereRevision = inventarioService.convertirItemsAConsumo(pedidoId, itemsPendientes);

        itemsPendientes.forEach(d -> d.setEstadoItem(EstadoItem.EN_PREPARACION));
        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));

        if (requiereRevision) {
            pedido.setRequiereRevision(true);
            // Corrección java:S2154 -> Casteo explícito a (Object) en pedidoId
            sseEmitterManager.publicarPorRol(empresaId, "ROLE_GERENTE", "PEDIDO_REQUIERE_REVISION", Map.of(
                    KEY_PEDIDO_ID, pedidoId,
                    KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : (Object) pedidoId,
                    "mensaje", "Stock insuficiente detectado al iniciar preparación"
            ));
        }
        pedidoRepository.save(pedido);

        sseEmitterManager.publicarTenant(empresaId, "EN_PREPARACION", Map.of(
                KEY_PEDIDO_ID, pedidoId,
            KEY_ESTADO, "EN_PREPARACION"
        ));
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PEDIDO + pedidoId + MSG_NO_ENCONTRADO));

        List<PedidoDetalle> itemsEnPreparacion = pedido.getDetalles().stream()
                .filter(d -> d.getEstadoItem() == EstadoItem.EN_PREPARACION)
            .toList();

        if (itemsEnPreparacion.isEmpty()) {
            throw new ReglaNegocioException("No hay ítems en preparación para marcar como listos.");
        }

        itemsEnPreparacion.forEach(d -> d.setEstadoItem(EstadoItem.LISTO));
        pedido.setEstadoActual(calcularEstadoAgregado(pedido.getDetalles()));
        pedidoRepository.save(pedido);

        Long empresaId = TenantContext.getCurrentTenant();
        Long mozoId = pedido.getMozo().getId();
        String mesa = pedido.getIdentificadorMesaReferencia() != null
                ? pedido.getIdentificadorMesaReferencia() : "";
        Map<String, Object> payload = Map.of(
                KEY_PEDIDO_ID, pedidoId,
            KEY_NUMERO_ORDEN, pedido.getNumeroOrden() != null ? pedido.getNumeroOrden() : (Object) pedidoId,
                "mesa", mesa,
                "tipoConsumo", pedido.getTipoConsumo() != null ? pedido.getTipoConsumo() : "",
            KEY_ESTADO, "LISTO"
        );

        sseEmitterManager.publicarUsuario(empresaId, mozoId, "PEDIDO_LISTO", payload);
        sseEmitterManager.publicarPorRol(empresaId, "ROLE_COCINA", "PEDIDO_LISTO", payload);
    }

    // R6-1: marcar AGOTADO_TEMPORAL — reversible desde cocina (E6-2)
    @Override
    @Transactional
    public void marcarAgotadoTemporal(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_TEMPORAL);
        productoRepository.save(producto);
        // R6-4: propagar a la pantalla del mozo en tiempo real
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_AGOTADO", Map.of(
            KEY_PRODUCTO_ID, productoId,
            KEY_NOMBRE, producto.getNombre(),
            KEY_ESTADO, "AGOTADO_TEMPORAL"
        ));
    }

    // R6-2: marcar AGOTADO_SERVICIO — bloqueado hasta cierre de caja (R6-3)
    @Override
    @Transactional
    public void marcarAgotadoServicio(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_SERVICIO);
        productoRepository.save(producto);
        // R6-4: propagar a la pantalla del mozo en tiempo real
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_AGOTADO", Map.of(
            KEY_PRODUCTO_ID, productoId,
            KEY_NOMBRE, producto.getNombre(),
            KEY_ESTADO, "AGOTADO_SERVICIO"
        ));
    }

    // E6-2: revertir AGOTADO_TEMPORAL → DISPONIBLE; AGOTADO_SERVICIO es intocable ítem por ítem
    @Override
    @Transactional
    public void revertirDisponible(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RecursoNoEncontradoException(PREFIX_PRODUCTO + productoId + MSG_NO_ENCONTRADO));
        if (producto.getEstadoDisponibilidad() == EstadoDisponibilidad.AGOTADO_SERVICIO) {
            throw new ReglaNegocioException(
                    "AGOTADO_SERVICIO no puede revertirse manualmente. Se restablece automáticamente en el cierre de caja.");
        }
        producto.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        productoRepository.save(producto);
        // R6-4: notificar al mozo que el producto vuelve a estar disponible
        sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "PRODUCTO_DISPONIBLE", Map.of(
            KEY_PRODUCTO_ID, productoId,
            KEY_NOMBRE, producto.getNombre()
        ));
    }

    // R6-5: porciones disponibles = min(stockDisponible / cantRequerida) por receta
    // Corrección java:S3776 -> Se extrajo la lógica de cálculo a un método privado para reducir la complejidad cognitiva
    @Override
    @Transactional(readOnly = true)
    public List<PorcionDisponibleDTO> calcularPorcionesDisponibles() {
        List<Producto> productos = productoRepository.findByEstadoRegistroTrue();
        List<PorcionDisponibleDTO> resultado = new ArrayList<>();

        for (Producto producto : productos) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(producto.getId());

            PorcionDisponibleDTO dto = new PorcionDisponibleDTO();
            dto.setProductoId(producto.getId());
            dto.setProducto(producto.getNombre());
            dto.setEstadoDisponibilidad(producto.getEstadoDisponibilidad().name());
            
            dto.setPorcionesDisponibles(calcularMinimoPorciones(receta));
            resultado.add(dto);
        }
        return resultado;
    }

    /**
     * Método auxiliar para calcular el mínimo de porciones basado en los requerimientos de la receta.
     */
    private BigDecimal calcularMinimoPorciones(List<RecetaDetalle> receta) {
        if (receta == null || receta.isEmpty()) {
            return null;
        }

        return receta.stream()
                .filter(rd -> rd.getCantidadRequerida().compareTo(BigDecimal.ZERO) > 0)
                .map(rd -> rd.getInsumo().getStockDisponible().divide(rd.getCantidadRequerida(), 2, RoundingMode.FLOOR))
                .min(BigDecimal::compareTo)
                .orElse(null);
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
}