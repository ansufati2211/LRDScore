package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;
import com.rutadelsabor.core.dto.response.InsumoFaltanteDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.exceptions.StockInsuficienteException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventarioServiceImpl implements IInventarioService {

    private static final String TIPO_MOVIMIENTO_RESERVA = "RESERVA";

    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final InsumoSedeRepository insumoSedeRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final PedidoDetalleRepository detalleRepository;

    public InventarioServiceImpl(CategoriaRepository categoriaRepository, InsumoRepository insumoRepository,
                                 InsumoSedeRepository insumoSedeRepository, ProductoRepository productoRepository,
                                 RecetaDetalleRepository recetaDetalleRepository, KardexMovimientoRepository kardexRepository,
                                 PedidoDetalleRepository detalleRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
        this.insumoSedeRepository = insumoSedeRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.kardexRepository = kardexRepository;
        this.detalleRepository = detalleRepository;
    }

    // --- ENCAPSULACIÓN JAVA 21 PARA FIX DE SONARLINT (S107) ---
    private record DatosKardex(
            Insumo insumo, String tipoMovimiento, BigDecimal cantidad,
            BigDecimal stockAnterior, BigDecimal stockPosterior,
            BigDecimal costoUnitario, Usuario usuario,
            String observacion, Long pedidoId
    ) {}

    // --- CATEGORIAS Y PRODUCTOS ---
    @Override @Transactional public Categoria crearCategoria(Categoria c) { return categoriaRepository.save(c); }
    @Override @Transactional public Categoria actualizarCategoria(Long id, CategoriaRequestDTO dto) {
        Categoria c = categoriaRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) c.setNombre(dto.getNombre());
        return categoriaRepository.save(c);
    }
    @Override @Transactional public void desactivarCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id).orElseThrow(); c.setEstadoRegistro(false); categoriaRepository.save(c);
    }
    @Override @Transactional public void activarCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id).orElseThrow(); c.setEstadoRegistro(true); categoriaRepository.save(c);
    }
    @Override @Transactional(readOnly = true) public List<Categoria> listarCategorias() { return categoriaRepository.findAll(); }

    @Override @Transactional public Producto crearProducto(Producto p) { return productoRepository.save(p); }
    @Override @Transactional public Producto actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto p = productoRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) p.setNombre(dto.getNombre());
        if (dto.getPrecioVenta() != null) p.setPrecioVenta(dto.getPrecioVenta());
        if (dto.getTagsBusqueda() != null) p.setTagsBusqueda(dto.getTagsBusqueda());
        if (dto.getCategoriaId() != null) p.setCategoria(categoriaRepository.findById(dto.getCategoriaId()).orElseThrow());
        if (dto.getEsPreparado() != null) p.setEsPreparado(dto.getEsPreparado());
        if (dto.getTiempoPreparacionMinutos() != null) p.setTiempoPreparacionMinutos(dto.getTiempoPreparacionMinutos());
        return productoRepository.save(p);
    }
    @Override @Transactional public void desactivarProducto(Long id) { Producto p = productoRepository.findById(id).orElseThrow(); p.setEstadoRegistro(false); productoRepository.save(p); }
    @Override @Transactional public void activarProducto(Long id) { Producto p = productoRepository.findById(id).orElseThrow(); p.setEstadoRegistro(true); productoRepository.save(p); }
    @Override @Transactional(readOnly = true) public List<Producto> listarProductos() { return productoRepository.findAll(); }

    // --- RECETAS ---
    @Override
    @Transactional
    public RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida) {
        Producto p = productoRepository.findById(productoId).orElseThrow();
        Insumo i = insumoRepository.findById(insumoId).orElseThrow();
        RecetaDetalle r = new RecetaDetalle(); r.setProducto(p); r.setInsumo(i); r.setCantidadRequerida(cantidad); r.setUnidadMedida(unidadMedida);
        return recetaDetalleRepository.save(r);
    }
    @Override @Transactional(readOnly = true) public List<RecetaDetalle> obtenerRecetaPorProducto(Long pId) { return recetaDetalleRepository.findByProductoId(pId); }

    // --- INSUMOS (SOLO CATÁLOGO) ---
    @Override @Transactional public Insumo crearInsumo(Insumo i) { return insumoRepository.save(i); }
    @Override @Transactional public Insumo actualizarInsumo(Long id, InsumoRequestDTO dto) {
        Insumo i = insumoRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) i.setNombre(dto.getNombre());
        if (dto.getUnidadMedida() != null) i.setUnidadMedida(dto.getUnidadMedida());
        return insumoRepository.save(i);
    }
    @Override @Transactional public void desactivarInsumo(Long id) { Insumo i = insumoRepository.findById(id).orElseThrow(); i.setEstadoRegistro(false); insumoRepository.save(i); }
    @Override @Transactional(readOnly = true) public List<Insumo> listarInsumos() { return insumoRepository.findAll(); }

    @Override 
    @Transactional(readOnly = true) 
    public List<InsumoBajoStockDTO> listarInsumosConStockBajo() {
        List<InsumoSede> insumosSede = insumoSedeRepository.findInsumosConStockBajoPorSede(TenantContext.getCurrentSede());
        
        return insumosSede.stream().map(is -> {
            InsumoBajoStockDTO dto = new InsumoBajoStockDTO();
            dto.setInsumoId(is.getInsumo().getId());
            dto.setNombre(is.getInsumo().getNombre());
            dto.setUnidadMedida(is.getInsumo().getUnidadMedida());
            dto.setSedeId(is.getSedeId());
            dto.setStockActual(is.getStockActual());
            dto.setStockMinimo(is.getStockMinimo());
            return dto;
        }).toList();
    }

    @Override 
    @Transactional(readOnly = true)
    public List<KardexMovimiento> listarKardexPorInsumo(Long insumoId) {
        return kardexRepository.findBySedeIdAndInsumoIdOrderByCreatedAtDesc(TenantContext.getCurrentSede(), insumoId);
    }

    // --- LÓGICA DE KARDEX FÍSICO POR SEDE ---
    
    private InsumoSede obtenerInventarioFisico(Long insumoId) {
        Long sedeId = TenantContext.getCurrentSede();
        return insumoSedeRepository.findBySedeIdAndInsumoId(sedeId, insumoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El insumo no tiene inventario inicializado en esta sede."));
    }

    @Override @Transactional
    public void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usr) {
        InsumoSede is = obtenerInventarioFisico(dto.getInsumoId());
        BigDecimal ant = is.getStockActual();
        BigDecimal post = ant.add(dto.getCantidad());

        BigDecimal valAnt = ant.multiply(is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO);
        BigDecimal valNuevo = dto.getCantidad().multiply(dto.getCostoUnitario());
        BigDecimal nuevoCosto = valAnt.add(valNuevo).divide(post, 2, RoundingMode.HALF_UP);

        is.setStockActual(post);
        is.setCostoUnitario(nuevoCosto);
        insumoSedeRepository.save(is);

        registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "ENTRADA_COMPRA", dto.getCantidad(), ant, post, nuevoCosto, usr, dto.getObservacion(), null));
    }

    @Override @Transactional
    public void registrarMerma(MermaRequestDTO dto, Usuario usr) {
        InsumoSede is = obtenerInventarioFisico(dto.getInsumoId());
        if (is.getStockActual().compareTo(dto.getCantidad()) < 0) throw new ReglaNegocioException("Stock físico insuficiente.");

        BigDecimal ant = is.getStockActual();
        BigDecimal post = ant.subtract(dto.getCantidad());
        is.setStockActual(post);
        insumoSedeRepository.save(is);

        registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "SALIDA_MERMA", dto.getCantidad(), ant, post, is.getCostoUnitario(), usr, dto.getMotivo(), null));
    }

    @Override @Transactional
    public void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usr) {
        InsumoSede is = obtenerInventarioFisico(dto.getInsumoId());
        BigDecimal ant = is.getStockActual();
        BigDecimal post = Boolean.TRUE.equals(dto.getEsPositivo()) ? ant.add(dto.getCantidad()) : ant.subtract(dto.getCantidad());

        if (post.compareTo(BigDecimal.ZERO) < 0) throw new ReglaNegocioException("Stock negativo.");
        is.setStockActual(post);
        insumoSedeRepository.save(is);
        String tipo = Boolean.TRUE.equals(dto.getEsPositivo()) ? "ENTRADA_AJUSTE" : "SALIDA_AJUSTE";
        
        registrarMovimientoKardex(new DatosKardex(is.getInsumo(), tipo, dto.getCantidad(), ant, post, is.getCostoUnitario(), usr, dto.getMotivo(), null));
    }

    // FIX SONARLINT (S107): Se consume el Record DatosKardex
    private void registrarMovimientoKardex(DatosKardex req) {
        KardexMovimiento k = new KardexMovimiento();
        k.setSedeId(TenantContext.getCurrentSede()); 
        k.setInsumo(req.insumo()); 
        k.setTipoMovimiento(req.tipoMovimiento()); 
        k.setCantidad(req.cantidad());
        k.setStockAnterior(req.stockAnterior()); 
        k.setStockPosterior(req.stockPosterior()); 
        k.setCostoUnitario(req.costoUnitario());
        k.setUsuario(req.usuario()); 
        k.setObservacion(req.observacion()); 
        k.setPedidoId(req.pedidoId());
        kardexRepository.save(k);
    }

    // --- MÓDULO 3: RESERVA Y CONSUMO ---

    @Override @Transactional
    public void reservarInsumosParaPedido(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo);
        List<InsumoFaltanteDTO> faltantes = validarDisponibilidadReserva(totalPorInsumo, insumosPorId);
        if (!faltantes.isEmpty()) throw new StockInsuficienteException(faltantes);
        
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            InsumoSede is = insumosPorId.get(entry.getKey());
            BigDecimal cant = entry.getValue();
            BigDecimal ant = is.getStockActual();
            is.setStockReservado(is.getStockReservado().add(cant));
            insumoSedeRepository.save(is);
            
            registrarMovimientoKardex(new DatosKardex(is.getInsumo(), TIPO_MOVIMIENTO_RESERVA, cant, ant, ant, is.getCostoUnitario(), null, "Reserva pedido #" + pedidoId, pedidoId));
        }
    }

    @Override @Transactional
    public void liberarReservaDePedido(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        for (KardexMovimiento r : reservas) {
            InsumoSede is = obtenerInventarioFisico(r.getInsumo().getId());
            BigDecimal cant = r.getCantidad();
            is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "LIBERACION_RESERVA", cant, is.getStockActual(), is.getStockActual(), is.getCostoUnitario(), null, "Liberación total pedido #" + pedidoId, pedidoId));
        }
    }

    @Override @Transactional
    public boolean convertirReservaAConsumo(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        boolean requiereRevision = false;
        for (KardexMovimiento r : reservas) {
            InsumoSede is = obtenerInventarioFisico(r.getInsumo().getId());
            BigDecimal cant = r.getCantidad();

            if (is.getStockActual().compareTo(cant) < 0) {
                requiereRevision = true;
                is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
                insumoSedeRepository.save(is);
                continue;
            }

            BigDecimal ant = is.getStockActual();
            BigDecimal post = ant.subtract(cant);
            is.setStockActual(post);
            is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "CONSUMO_PRODUCCION", cant, ant, post, is.getCostoUnitario(), null, "Consumo producción pedido #" + pedidoId, pedidoId));
        }
        return requiereRevision;
    }

    // --- MÓDULO 4: MULTI-COMANDA ---

    @Override @Transactional
    public boolean convertirItemsAConsumo(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo);
        Map<Long, BigDecimal> costoUnitarioPorDetalle = calcularCostoUnitarioPorDetalle(detalles, insumosPorId);
        
        boolean reqRevision = consumirInsumos(pedidoId, totalPorInsumo, insumosPorId);
        persistirCostoUnitarioConsumido(detalles, costoUnitarioPorDetalle);
        return reqRevision;
    }

    @Override @Transactional
    public void liberarReservaDeItems(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo);
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            InsumoSede is = insumosPorId.get(entry.getKey());
            BigDecimal cant = entry.getValue();
            is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "LIBERACION_RESERVA", cant, is.getStockActual(), is.getStockActual(), is.getCostoUnitario(), null, "Liberación parcial ítem en pedido #" + pedidoId, pedidoId));
        }
    }

    // --- Helpers de Cálculo ---

    private Map<Long, BigDecimal> calcularTotalPorInsumo(List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> total = new LinkedHashMap<>();
        for (PedidoDetalle d : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(d.getProducto().getId());
            BigDecimal factor = new BigDecimal(d.getCantidad());
            for (RecetaDetalle rd : receta) total.merge(rd.getInsumo().getId(), rd.getCantidadRequerida().multiply(factor), BigDecimal::add);
        }
        return total;
    }

    private Map<Long, InsumoSede> cargarInventarioSede(Map<Long, BigDecimal> totalPorInsumo) {
        Map<Long, InsumoSede> map = new LinkedHashMap<>();
        Long sedeId = TenantContext.getCurrentSede();
        for (Long insumoId : totalPorInsumo.keySet()) {
            insumoSedeRepository.findBySedeIdAndInsumoId(sedeId, insumoId).ifPresent(is -> map.put(insumoId, is));
        }
        return map;
    }

    private List<InsumoFaltanteDTO> validarDisponibilidadReserva(Map<Long, BigDecimal> req, Map<Long, InsumoSede> inv) {
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : req.entrySet()) {
            InsumoSede is = inv.get(entry.getKey());
            if (is == null) continue;
            BigDecimal necesario = entry.getValue();
            BigDecimal disp = is.getStockActual().subtract(is.getStockReservado());
            if (disp.compareTo(necesario) < 0) faltantes.add(new InsumoFaltanteDTO(is.getInsumo().getNombre(), disp.max(BigDecimal.ZERO), necesario));
        }
        return faltantes;
    }

    private Map<Long, BigDecimal> calcularCostoUnitarioPorDetalle(List<PedidoDetalle> detalles, Map<Long, InsumoSede> inv) {
        Map<Long, BigDecimal> costoDetalle = new LinkedHashMap<>();
        for (PedidoDetalle d : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(d.getProducto().getId());
            if (receta.isEmpty()) {
                BigDecimal cr = d.getProducto().getCostoReferencial();
                costoDetalle.put(d.getId(), cr != null ? cr : BigDecimal.ZERO);
                continue;
            }
            BigDecimal costoItem = BigDecimal.ZERO;
            for (RecetaDetalle rd : receta) {
                InsumoSede is = inv.get(rd.getInsumo().getId());
                if (is != null && is.getCostoUnitario() != null) costoItem = costoItem.add(rd.getCantidadRequerida().multiply(is.getCostoUnitario()));
            }
            costoDetalle.put(d.getId(), costoItem);
        }
        return costoDetalle;
    }

    private boolean consumirInsumos(Long pedidoId, Map<Long, BigDecimal> req, Map<Long, InsumoSede> inv) {
        boolean reqRev = false;
        for (Map.Entry<Long, BigDecimal> entry : req.entrySet()) {
            InsumoSede is = inv.get(entry.getKey());
            if (is == null) continue;
            BigDecimal cant = entry.getValue();
            if (is.getStockActual().compareTo(cant) < 0) {
                reqRev = true;
                is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
            } else {
                BigDecimal ant = is.getStockActual();
                BigDecimal post = ant.subtract(cant);
                is.setStockActual(post);
                is.setStockReservado(is.getStockReservado().subtract(cant).max(BigDecimal.ZERO));
                
                registrarMovimientoKardex(new DatosKardex(is.getInsumo(), "CONSUMO_PRODUCCION", cant, ant, post, is.getCostoUnitario(), null, "Consumo pedido #" + pedidoId, pedidoId));
            }
            insumoSedeRepository.save(is);
        }
        return reqRev;
    }

    private void persistirCostoUnitarioConsumido(List<PedidoDetalle> detalles, Map<Long, BigDecimal> costoDetalle) {
        for (PedidoDetalle d : detalles) {
            BigDecimal c = costoDetalle.get(d.getId());
            if (c != null) { d.setCostoUnitarioConsumido(c); detalleRepository.save(d); }
        }
    }
}