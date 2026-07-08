package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.response.KdsCocinaDTO;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KdsServiceImpl implements IKdsService {

    private final VwKdsCocinaRepository vwKdsCocinaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final InsumoSedeRepository insumoSedeRepository;
    private final SedeRepository sedeRepository;
    private final SseEmitterManager sseEmitterManager;

    public KdsServiceImpl(VwKdsCocinaRepository vwKdsCocinaRepository,
                          PedidoRepository pedidoRepository,
                          ProductoRepository productoRepository,
                          RecetaDetalleRepository recetaDetalleRepository,
                          InsumoSedeRepository insumoSedeRepository,
                          SedeRepository sedeRepository,
                          SseEmitterManager sseEmitterManager) {
        this.vwKdsCocinaRepository = vwKdsCocinaRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.insumoSedeRepository = insumoSedeRepository;
        this.sedeRepository = sedeRepository;
        this.sseEmitterManager = sseEmitterManager;
    }

    private void validarSedeDeEmpresa(Long sedeIdFiltro) {
        Sede sede = sedeRepository.findById(sedeIdFiltro)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        if (!sede.getEmpresaId().equals(TenantContext.getCurrentTenant())) {
            throw new ReglaNegocioException("La sede indicada no pertenece a su empresa.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<KdsCocinaDTO> obtenerPedidosPendientes(Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        List<VwKdsCocina> items;
        
        if (sedeId != null) {
            items = vwKdsCocinaRepository.findBySedeIdOrderByHoraIngresoAsc(sedeId);
        } else {
            if (sedeIdFiltro != null) {
                validarSedeDeEmpresa(sedeIdFiltro);
                items = vwKdsCocinaRepository.findBySedeIdOrderByHoraIngresoAsc(sedeIdFiltro);
            } else {
                items = vwKdsCocinaRepository.findAllByOrderByHoraIngresoAsc();
            }
        }

        Map<Long, List<VwKdsCocina>> agrupados = items.stream().collect(Collectors.groupingBy(VwKdsCocina::getPedidoId));

        return agrupados.entrySet().stream().map(entry -> {
            VwKdsCocina primer = entry.getValue().get(0);
            KdsCocinaDTO dto = new KdsCocinaDTO();
            dto.setPedidoId(primer.getPedidoId());
            dto.setNumeroOrden(primer.getNumeroOrden());
            dto.setTipoConsumo(primer.getTipoConsumo());
            dto.setMesa(primer.getMesa());
            dto.setEstadoPedido(primer.getEstadoPedido());
            dto.setNotasGenerales(primer.getNotasGenerales());
            dto.setHoraIngreso(primer.getHoraIngreso());
            dto.setMinutosTranscurridos(primer.getMinutosTranscurridos());

            dto.setItems(entry.getValue().stream().map(v -> {
                KdsCocinaDTO.KdsItemDTO item = new KdsCocinaDTO.KdsItemDTO();
                item.setDetalleId(v.getDetalleId());
                item.setProducto(v.getProducto());
                item.setCantidad(v.getCantidad());
                item.setNotasPreparacion(v.getNotasPreparacion());
                item.setTiempoPreparacionMinutos(v.getTiempoPreparacionMinutos());
                item.setEstadoItem(v.getEstadoItem());
                item.setNumeroComanda(v.getNumeroComanda());
                return item;
            }).toList());
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public void marcarPreparando(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        if (pedido.getEstadoActual() != EstadoPedido.RECIBIDO) throw new ReglaNegocioException("El pedido debe estar RECIBIDO.");
        
        pedidoRepository.iniciarPreparacionYDescontarStock(pedidoId, usuarioId);
        pedido.setEstadoActual(EstadoPedido.EN_PREPARACION);
        
        // FASE 7: Emisión aislada a la sede
        sseEmitterManager.publicarTenantYSede(pedido.getEmpresaId(), pedido.getSedeId(), "PEDIDO_PREPARANDO", Map.of("pedidoId", pedidoId));
    }

    @Override
    @Transactional
    public void marcarListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        if (pedido.getEstadoActual() != EstadoPedido.EN_PREPARACION) throw new ReglaNegocioException("El pedido debe estar EN_PREPARACION.");
        
        pedido.setEstadoActual(EstadoPedido.LISTO);
        pedido.getDetalles().forEach(d -> {
            if (d.getEstadoItem() == EstadoItem.EN_PREPARACION) d.setEstadoItem(EstadoItem.LISTO);
        });
        pedidoRepository.save(pedido);

        // FASE 7: Emisión aislada a la sede
        sseEmitterManager.publicarTenantYSede(pedido.getEmpresaId(), pedido.getSedeId(), "PEDIDO_LISTO", Map.of(
                "pedidoId", pedido.getId(),
                "mesa", pedido.getIdentificadorMesaReferencia() != null ? pedido.getIdentificadorMesaReferencia() : ""
        ));
    }

    // --- MÉTODOS DE DISPONIBILIDAD (Agotados) ---

    @Override @Transactional
    public void marcarAgotadoTemporal(Long productoId) { cambiarDisponibilidad(productoId, EstadoDisponibilidad.AGOTADO_TEMPORAL); }
    @Override @Transactional
    public void marcarAgotadoServicio(Long productoId) { cambiarDisponibilidad(productoId, EstadoDisponibilidad.AGOTADO_SERVICIO); }
    @Override @Transactional
    public void revertirDisponible(Long productoId) {
        Producto p = productoRepository.findById(productoId).orElseThrow();
        if (p.getEstadoDisponibilidad() == EstadoDisponibilidad.AGOTADO_SERVICIO) throw new ReglaNegocioException("No puede revertirse manualmente.");
        p.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        productoRepository.save(p);
    }

    private void cambiarDisponibilidad(Long productoId, EstadoDisponibilidad estado) {
        Producto p = productoRepository.findById(productoId).orElseThrow();
        p.setEstadoDisponibilidad(estado);
        productoRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorcionDisponibleDTO> calcularPorcionesDisponibles(Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        Long sedeAEvaluar = (sedeId != null) ? sedeId : sedeIdFiltro;
        if (sedeAEvaluar == null) return Collections.emptyList(); 

        List<Producto> productos = productoRepository.findByEstadoRegistroTrue();
        return productos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getEsPreparado()))
                .map(p -> {
                    List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(p.getId());
                    BigDecimal minPorciones = calcularMinimoPorciones(receta, sedeAEvaluar);
                    if (minPorciones == null) return null;
                    
                    PorcionDisponibleDTO dto = new PorcionDisponibleDTO();
                    dto.setProductoId(p.getId());
                    dto.setNombreProducto(p.getNombre());
                    dto.setPorcionesDisponibles(minPorciones.intValue());
                    dto.setNivelAdvertencia(minPorciones.intValue() <= 5 ? "ALTO" : "NORMAL");
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private BigDecimal calcularMinimoPorciones(List<RecetaDetalle> receta, Long sedeAEvaluar) {
        if (receta == null || receta.isEmpty()) return null;
        return receta.stream()
                .filter(rd -> rd.getCantidadRequerida().compareTo(BigDecimal.ZERO) > 0)
                .map(rd -> {
                    InsumoSede is = insumoSedeRepository.findBySedeIdAndInsumoId(sedeAEvaluar, rd.getInsumo().getId()).orElse(null);
                    if (is == null) return BigDecimal.ZERO;
                    BigDecimal disp = is.getStockActual().subtract(is.getStockReservado());
                    return disp.divide(rd.getCantidadRequerida(), 2, RoundingMode.FLOOR);
                })
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
}